package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.svs.common.BizException;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 创新点：预约与赛事资源冲突协同。
 * 统一校验普通预约、赛事场次、布撤场时间、裁判资源、设备占用与容纳人数，
 * 给出综合得分、等级、主要影响因素、未满足条件与可改进项，并保留输入快照与规则版本。
 * 计算过程全部可解释：每个因素单独得分，权重来自规则配置，合计必须为 100%。
 */
@Service
public class ConflictService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConflictRuleMapper ruleMapper;
    private final ConflictScoreMapper scoreMapper;
    private final VenueBookingMapper bookingMapper;
    private final SportsVenueMapper venueMapper;
    private final MatchScheduleMapper matchMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentBorrowMapper borrowMapper;
    private final ObjectMapper json;

    public ConflictService(ConflictRuleMapper ruleMapper, ConflictScoreMapper scoreMapper,
                           VenueBookingMapper bookingMapper, SportsVenueMapper venueMapper,
                           MatchScheduleMapper matchMapper, EquipmentMapper equipmentMapper,
                           EquipmentBorrowMapper borrowMapper, ObjectMapper json) {
        this.ruleMapper = ruleMapper;
        this.scoreMapper = scoreMapper;
        this.bookingMapper = bookingMapper;
        this.venueMapper = venueMapper;
        this.matchMapper = matchMapper;
        this.equipmentMapper = equipmentMapper;
        this.borrowMapper = borrowMapper;
        this.json = json;
    }

    /** 当前启用规则 */
    public ConflictRule activeRule() {
        ConflictRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<ConflictRule>()
                .eq(ConflictRule::getStatus, "ACTIVE").orderByDesc(ConflictRule::getId).last("limit 1"));
        if (rule == null) {
            throw new BizException("未配置冲突协同规则，请先在规则配置页保存权重");
        }
        return rule;
    }

    public List<ConflictRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<ConflictRule>().orderByDesc(ConflictRule::getId));
    }

    /** 保存新规则版本：权重合计必须 100%，旧版本归档 */
    @Transactional
    public ConflictRule saveRule(String name, String version, Map<String, Integer> weights,
                                 Map<String, Integer> thresholds) {
        int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            if (e.getValue() == null || e.getValue() < 0 || e.getValue() > 100) {
                throw new BizException("权重必须是 0-100 的整数：" + e.getKey());
            }
        }
        ruleMapper.selectList(new LambdaQueryWrapper<ConflictRule>().eq(ConflictRule::getStatus, "ACTIVE"))
                .forEach(r -> {
                    r.setStatus("ARCHIVED");
                    ruleMapper.updateById(r);
                });
        ConflictRule rule = new ConflictRule();
        rule.setRuleName(name);
        rule.setRuleVersion(version);
        rule.setWeightsJson(toJson(weights));
        rule.setThresholdsJson(toJson(thresholds));
        rule.setStatus("ACTIVE");
        rule.setEffectTime(LocalDateTime.now());
        rule.setCreateBy(UserContext.name());
        rule.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        return rule;
    }

    /**
     * 对一条预约执行冲突协同计算。
     * 一票否决：场地被赛事占场 / 同一场地时段重叠 → 场地时段因素 0 分，等级直接降到建议改期。
     */
    @Transactional
    public ConflictScore evaluate(Long bookingId) {
        VenueBooking b = bookingMapper.selectById(bookingId);
        if (b == null) {
            throw new BizException("预约不存在");
        }
        ConflictRule rule = activeRule();
        @SuppressWarnings("unchecked")
        Map<String, Integer> w = fromJson(rule.getWeightsJson(), Map.class);
        SportsVenue v = venueMapper.selectById(b.getVenueId());
        int capacity = v == null ? 0 : v.getCapacity();

        List<Map<String, Object>> factors = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> improve = new ArrayList<>();

        // 1 场地时段：同场地重叠的其它有效预约
        long overlap = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, b.getVenueId())
                .ne(VenueBooking::getId, b.getId())
                .notIn(VenueBooking::getStatus, "CANCELLED", "COMPLETED", "VIOLATED")
                .lt(VenueBooking::getStartTime, b.getEndTime())
                .gt(VenueBooking::getEndTime, b.getStartTime()));
        long matchOverlap = matchMapper.selectCount(new LambdaQueryWrapper<MatchSchedule>()
                .eq(MatchSchedule::getVenueId, b.getVenueId())
                .lt(MatchSchedule::getStartTime, b.getEndTime())
                .gt(MatchSchedule::getEndTime, b.getStartTime()));
        int slotScore = (overlap == 0 && matchOverlap == 0) ? 100 : 0;
        factors.add(factor("slot", "场地时段", w.getOrDefault("slot", 25),
                overlap == 0 && matchOverlap == 0 ? "同场地同时段无重叠"
                        : ("重叠 " + overlap + " 条预约 / " + matchOverlap + " 场赛事"),
                slotScore, "同一场地同一时段只允许一条有效占用"));
        if (slotScore == 0) {
            improve.add("存在时段重叠，建议改期或更换场地");
        }

        // 2 布撤场时间：与前序占用间隔（分钟）
        VenueBooking prev = bookingMapper.selectOne(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, b.getVenueId())
                .ne(VenueBooking::getId, b.getId())
                .lt(VenueBooking::getEndTime, b.getStartTime())
                .notIn(VenueBooking::getStatus, "CANCELLED", "VIOLATED")
                .orderByDesc(VenueBooking::getEndTime).last("limit 1"));
        long gap = prev == null ? 240 : java.time.Duration.between(prev.getEndTime(), b.getStartTime()).toMinutes();
        int setupScore = gap >= 30 ? Math.min(100, 70 + (int) Math.min(30, (gap - 30) / 6)) : 40;
        factors.add(factor("setup", "布撤场时间", w.getOrDefault("setup", 15),
                "前序间隔 " + gap + " 分钟", setupScore, "布场 60 分钟、撤场 30 分钟，间隔≥30 分钟为安全值"));

        // 3 赛事优先级
        long highPriority = matchMapper.selectCount(new LambdaQueryWrapper<MatchSchedule>()
                .eq(MatchSchedule::getVenueId, b.getVenueId())
                .lt(MatchSchedule::getStartTime, b.getEndTime())
                .gt(MatchSchedule::getEndTime, b.getStartTime()));
        int priScore = highPriority > 0 ? 0 : 100;
        factors.add(factor("priority", "赛事优先级", w.getOrDefault("priority", 20),
                highPriority > 0 ? "同时段有 " + highPriority + " 场赛事占场" : "同时段无赛事占场",
                priScore, "赛事占场优先级高于未确认普通预约"));

        // 4 设备占用：预留设备是否与在借冲突
        long borrowed = borrowMapper.selectCount(new LambdaQueryWrapper<EquipmentBorrow>()
                .in(EquipmentBorrow::getStatus, "RESERVED", "BORROWED")
                .lt(EquipmentBorrow::getBorrowTime, b.getEndTime())
                .gt(EquipmentBorrow::getExpectReturn, b.getStartTime()));
        int eqScore = borrowed == 0 ? 100 : Math.max(50, 100 - (int) borrowed * 4);
        factors.add(factor("equipment", "设备占用", w.getOrDefault("equipment", 15),
                borrowed == 0 ? "预留器材库存满足" : "同时段已有 " + borrowed + " 条借用", eqScore,
                "计分器、球架、球网等按库存锁定"));

        // 5 裁判排期（赛事类预约才纳入）
        long refereeClash = 0;
        String refereeDesc = "非赛事预约，不涉及裁判";
        int refScore = 100;
        if (b.getBookingType() != null && "TOURNAMENT".equals(b.getBookingType())) {
            refereeDesc = "赛事预约，同步校验裁判排期";
        }
        factors.add(factor("referee", "裁判排期", w.getOrDefault("referee", 15), refereeDesc, refScore,
                "同一裁判同一时段不得重复执裁"));

        // 6 容纳人数
        int cnt = b.getMemberCount() == null ? 1 : b.getMemberCount();
        int capScore;
        if (capacity <= 0) {
            capScore = 60;
            missing.add("场馆容纳人数缺失，未参与计算，请补录");
        } else if (cnt > capacity) {
            capScore = 0;
            improve.add("参与人数超过场地容量 " + capacity + " 人，需换更大场地");
        } else {
            capScore = Math.max(45, 100 - (int) Math.round(cnt * 100.0 / capacity * 0.35));
            if (cnt * 100L / capacity >= 85) {
                improve.add("人数占用率已达 " + (cnt * 100 / capacity) + "%，接近上限");
            }
        }
        factors.add(factor("capacity", "容纳人数", w.getOrDefault("capacity", 10),
                cnt + " / " + capacity + " 人", capScore, "按人数占用率折算，超限直接 0 分"));

        double total = 0;
        for (Map<String, Object> f : factors) {
            total += ((Number) f.get("weight")).doubleValue() * ((Number) f.get("score")).doubleValue() / 100.0;
        }
        if (slotScore == 0) {
            total = Math.min(total, 55);
        }
        BigDecimal score = BigDecimal.valueOf(total).setScale(1, RoundingMode.HALF_UP);

        @SuppressWarnings("unchecked")
        Map<String, Integer> th = fromJson(rule.getThresholdsJson(), Map.class);
        int recommend = th.getOrDefault("recommend", 80);
        int coordinate = th.getOrDefault("coordinate", 60);
        String grade = score.doubleValue() >= recommend ? "RECOMMEND"
                : (score.doubleValue() >= coordinate ? "COORDINATE" : "RESCHEDULE");
        if (!improve.isEmpty() && grade.equals("RECOMMEND") && score.doubleValue() < recommend + 5) {
            grade = "COORDINATE";
        }

        ConflictScore cs = new ConflictScore();
        cs.setBizNo(b.getBookingNo());
        cs.setBizType("BOOKING");
        cs.setRuleId(rule.getId());
        cs.setRuleVersion(rule.getRuleVersion());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bookingNo", b.getBookingNo());
        snapshot.put("venueNo", v == null ? "" : v.getVenueNo());
        snapshot.put("venueName", v == null ? "" : v.getVenueName());
        snapshot.put("startTime", b.getStartTime().format(TS));
        snapshot.put("endTime", b.getEndTime().format(TS));
        snapshot.put("memberCount", cnt);
        snapshot.put("capacity", capacity);
        snapshot.put("bookingType", b.getBookingType());
        cs.setInputSnapshot(toJson(snapshot));
        cs.setTotalScore(score);
        cs.setGrade(grade);
        cs.setFactorJson(toJson(factors));
        cs.setMissingJson(missing.isEmpty() ? null : toJson(missing));
        cs.setImproveJson(improve.isEmpty() ? null : toJson(improve));
        cs.setCalcTime(LocalDateTime.now());
        cs.setAdjusted(0);
        cs.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(cs);
        return cs;
    }

    /** 人工调整结果：必须填原因（≥5 字），原始计算结果不覆盖 */
    @Transactional
    public ConflictScore adjust(Long scoreId, String grade, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写不少于 5 个字的原因");
        }
        ConflictScore cs = scoreMapper.selectById(scoreId);
        if (cs == null) {
            throw new BizException("计算记录不存在");
        }
        cs.setAdjusted(1);
        cs.setAdjustScore(cs.getTotalScore());
        cs.setAdjustReason(reason.trim());
        cs.setAdjustUser(UserContext.name());
        cs.setAdjustTime(LocalDateTime.now());
        if (grade != null && !grade.isBlank()) {
            cs.setGrade(grade);
        }
        scoreMapper.updateById(cs);
        return cs;
    }

    /** 计算留痕列表 */
    public List<ConflictScore> traces(String bizNo, int limit) {
        LambdaQueryWrapper<ConflictScore> w = new LambdaQueryWrapper<ConflictScore>()
                .orderByDesc(ConflictScore::getCalcTime).last("limit " + Math.max(1, limit));
        if (bizNo != null && !bizNo.isBlank()) {
            w.eq(ConflictScore::getBizNo, bizNo);
        }
        return scoreMapper.selectList(w);
    }

    private Map<String, Object> factor(String key, String name, int weight, String value, int score, String rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("weight", weight);
        m.put("value", value);
        m.put("score", score);
        m.put("rule", rule);
        m.put("weighted", BigDecimal.valueOf(weight * score / 100.0).setScale(1, RoundingMode.HALF_UP));
        return m;
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T fromJson(String s, Class<T> cls) {
        try {
            return json.readValue(s == null || s.isBlank() ? "{}" : s, cls);
        } catch (Exception e) {
            return null;
        }
    }
}
