package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.svs.common.BizException;
import com.grad.svs.common.PageResult;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 预约全流程：提交（锁名额 + 冲突预检）→ 团体审批 → 场地确认 → 入场核验 → 离场检查 → 完成；
 * 取消释放名额并触发候补转正；未到登记生成违约记录。
 * 所有状态变更由后端校验，前端按钮只是入口。
 */
@Service
public class BookingService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VenueBookingMapper bookingMapper;
    private final VenueSlotMapper slotMapper;
    private final SportsVenueMapper venueMapper;
    private final BookingWaitlistMapper waitlistMapper;
    private final BookingMemberMapper memberMapper;
    private final FlowLogMapper flowMapper;
    private final ViolationRecordMapper violationMapper;
    private final EquipmentBorrowMapper borrowMapper;
    private final ConflictService conflictService;

    public BookingService(VenueBookingMapper bookingMapper, VenueSlotMapper slotMapper, SportsVenueMapper venueMapper,
                          BookingWaitlistMapper waitlistMapper, BookingMemberMapper memberMapper, FlowLogMapper flowMapper,
                          ViolationRecordMapper violationMapper, EquipmentBorrowMapper borrowMapper,
                          ConflictService conflictService) {
        this.bookingMapper = bookingMapper;
        this.slotMapper = slotMapper;
        this.venueMapper = venueMapper;
        this.waitlistMapper = waitlistMapper;
        this.memberMapper = memberMapper;
        this.flowMapper = flowMapper;
        this.violationMapper = violationMapper;
        this.borrowMapper = borrowMapper;
        this.conflictService = conflictService;
    }

    public PageResult<VenueBooking> page(int page, int size, String status, String sportType, Long venueId,
                                         String keyword, String tab) {
        LambdaQueryWrapper<VenueBooking> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(VenueBooking::getStatus, status);
        }
        if ("TODO".equals(tab)) {
            w.in(VenueBooking::getStatus, "PENDING_APPROVAL", "PENDING_CONFIRM");
        } else if ("RUNNING".equals(tab)) {
            w.in(VenueBooking::getStatus, "PENDING_ENTRY", "IN_USE", "LEAVING");
        } else if ("DONE".equals(tab)) {
            w.eq(VenueBooking::getStatus, "COMPLETED");
        } else if ("ABNORMAL".equals(tab)) {
            w.in(VenueBooking::getStatus, "VIOLATED", "CANCELLED");
        }
        if (venueId != null) {
            w.eq(VenueBooking::getVenueId, venueId);
        }
        if (sportType != null && !sportType.isBlank()) {
            List<Long> ids = new ArrayList<>();
            venueMapper.selectList(new LambdaQueryWrapper<SportsVenue>().eq(SportsVenue::getSportType, sportType))
                    .forEach(v -> ids.add(v.getId()));
            if (ids.isEmpty()) {
                return new PageResult<>(0, page, size, List.of());
            }
            w.in(VenueBooking::getVenueId, ids);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(VenueBooking::getBookingNo, keyword).or().like(VenueBooking::getTeamName, keyword)
                    .or().like(VenueBooking::getPurpose, keyword));
        }
        // 待本人处理优先、更新时间倒序
        w.orderByAsc(VenueBooking::getStatus).orderByDesc(VenueBooking::getUpdateTime);
        Page<VenueBooking> p = bookingMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public Map<String, Object> detail(Long id) {
        VenueBooking b = bookingMapper.selectById(id);
        if (b == null) {
            throw new BizException("预约不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", b);
        m.put("venue", venueMapper.selectById(b.getVenueId()));
        m.put("members", memberMapper.selectList(new LambdaQueryWrapper<BookingMember>()
                .eq(BookingMember::getBookingId, id)));
        m.put("flows", flowMapper.selectList(new LambdaQueryWrapper<FlowLog>()
                .eq(FlowLog::getBizType, "BOOKING").eq(FlowLog::getBizId, id).orderByAsc(FlowLog::getCreateTime)));
        m.put("borrows", borrowMapper.selectList(new LambdaQueryWrapper<EquipmentBorrow>()
                .eq(EquipmentBorrow::getBookingId, id)));
        m.put("score", conflictService.traces(b.getBookingNo(), 1).stream().findFirst().orElse(null));
        return m;
    }

    /** 新建预约：校验容量、锁定名额、写入留痕，团体预约进入待审批 */
    @Transactional
    public VenueBooking create(VenueBooking b) {
        if (b.getVenueId() == null || b.getStartTime() == null || b.getEndTime() == null) {
            throw new BizException("场馆、开始时间与结束时间必填");
        }
        if (!b.getEndTime().isAfter(b.getStartTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        SportsVenue v = venueMapper.selectById(b.getVenueId());
        if (v == null) {
            throw new BizException("场馆不存在");
        }
        if (!"NORMAL".equals(v.getStatus())) {
            throw new BizException("场馆当前状态为维护中或停用，暂不可预约");
        }
        int cnt = b.getMemberCount() == null ? 1 : b.getMemberCount();
        if (cnt < 1) {
            throw new BizException("参与人数至少 1 人");
        }
        if (cnt > v.getCapacity()) {
            throw new BizException("参与人数超过场地容量上限 " + v.getCapacity() + " 人");
        }
        // 同一场地时段冲突
        long overlap = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, b.getVenueId())
                .notIn(VenueBooking::getStatus, "CANCELLED", "VIOLATED")
                .lt(VenueBooking::getStartTime, b.getEndTime())
                .gt(VenueBooking::getEndTime, b.getStartTime()));
        if (overlap > 0) {
            throw new BizException("该场地在该时段已被占用（冲突 " + overlap + " 条），请选择其它时段或加入候补");
        }
        b.setBookingNo("BK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%02d", new Random().nextInt(99)));
        b.setUserId(UserContext.userId());
        b.setCapacityLimit(v.getCapacity());
        b.setMemberCount(cnt);
        long hours = java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours();
        BigDecimal price = v.getPriceStudent() == null ? BigDecimal.ZERO : v.getPriceStudent();
        b.setFeeAmount(price.multiply(BigDecimal.valueOf(Math.max(1, hours))));
        b.setFeeStatus("UNPAID");
        b.setBookingCode("TY-" + Long.toHexString(System.currentTimeMillis()).toUpperCase().substring(8)
                + "-" + String.format("%04X", new Random().nextInt(65535)));
        boolean team = "TEAM".equals(b.getBookingType()) || cnt > 10 || hours > 3;
        b.setBookingType(team ? "TEAM" : "PERSONAL");
        b.setStatus(team ? "PENDING_APPROVAL" : "PENDING_CONFIRM");
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        b.setVersion(0);
        bookingMapper.insert(b);
        flow(b, null, b.getStatus(), "提交预约申请", team ? "团体预约需场馆工作人员审批" : "个人预约提交成功");
        // 锁定名额（时段容量扣减）
        if (b.getSlotId() != null) {
            VenueSlot s = slotMapper.selectById(b.getSlotId());
            if (s != null) {
                s.setBookedCount(Math.min(s.getCapacity(), (s.getBookedCount() == null ? 0 : s.getBookedCount()) + cnt));
                if (s.getBookedCount() >= s.getCapacity()) {
                    s.setStatus("FULL");
                }
                slotMapper.updateById(s);
            }
        }
        return b;
    }

    /** 状态流转：approve/confirm/checkin/leave/complete/cancel/no-show */
    @Transactional
    public VenueBooking transition(Long id, String action, String reason, Integer actualCount) {
        VenueBooking b = bookingMapper.selectById(id);
        if (b == null) {
            throw new BizException("预约不存在");
        }
        String from = b.getStatus();
        String to;
        switch (action) {
            case "approve" -> {
                need(from, "PENDING_APPROVAL");
                to = "PENDING_CONFIRM";
            }
            case "reject" -> {
                need(from, "PENDING_APPROVAL");
                needReason(reason);
                to = "CANCELLED";
            }
            case "confirm" -> {
                need(from, "PENDING_CONFIRM");
                to = "PENDING_ENTRY";
            }
            case "checkin" -> {
                need(from, "PENDING_ENTRY");
                to = "IN_USE";
            }
            case "leave" -> {
                need(from, "IN_USE");
                to = "LEAVING";
            }
            case "complete" -> {
                need(from, "LEAVING");
                to = "COMPLETED";
            }
            case "cancel" -> {
                if (List.of("COMPLETED", "CANCELLED", "VIOLATED").contains(from)) {
                    throw new BizException("该预约已结束，不能取消");
                }
                needReason(reason);
                to = "CANCELLED";
            }
            case "no-show" -> {
                need(from, "PENDING_ENTRY");
                needReason(reason);
                to = "VIOLATED";
            }
            default -> throw new BizException("不支持的操作：" + action);
        }
        b.setStatus(to);
        b.setUpdateTime(LocalDateTime.now());
        b.setVersion(b.getVersion() == null ? 1 : b.getVersion() + 1);
        if ("checkin".equals(action) && actualCount != null) {
            b.setMemberCount(actualCount);
        }
        bookingMapper.updateById(b);
        flow(b, from, to, actionName(action), reason == null ? defaultReason(action) : reason);

        if ("CANCELLED".equals(to) || "VIOLATED".equals(to)) {
            releaseSlot(b);
        }
        if ("CANCELLED".equals(to)) {
            promoteWaitlist(b);
        }
        if ("VIOLATED".equals(to)) {
            ViolationRecord vr = new ViolationRecord();
            vr.setViolationNo("VI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            vr.setUserId(b.getUserId());
            vr.setUserName(b.getTeamName());
            vr.setViolationType("NO_SHOW");
            vr.setBizNo(b.getBookingNo());
            vr.setOccurDate(LocalDate.now());
            vr.setPunish("限制预约 7 天");
            vr.setCreditDelta(-10);
            vr.setStatus("EFFECTIVE");
            vr.setCreateTime(LocalDateTime.now());
            violationMapper.insert(vr);
        }
        return b;
    }

    /** 按预约码核验：返回单据供工作人员核对时段与场地 */
    public Map<String, Object> verify(String code) {
        VenueBooking b = bookingMapper.selectOne(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getBookingCode, code).last("limit 1"));
        if (b == null) {
            throw new BizException("预约码无效，请核对后重试");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("booking", b);
        m.put("venue", venueMapper.selectById(b.getVenueId()));
        m.put("slotMatched", "PENDING_ENTRY".equals(b.getStatus()));
        return m;
    }

    private void releaseSlot(VenueBooking b) {
        if (b.getSlotId() == null) {
            return;
        }
        VenueSlot s = slotMapper.selectById(b.getSlotId());
        if (s == null) {
            return;
        }
        int cnt = b.getMemberCount() == null ? 0 : b.getMemberCount();
        s.setBookedCount(Math.max(0, (s.getBookedCount() == null ? 0 : s.getBookedCount()) - cnt));
        if ("FULL".equals(s.getStatus()) && s.getBookedCount() < s.getCapacity()) {
            s.setStatus("OPEN");
        }
        slotMapper.updateById(s);
    }

    /** 取消后按队列顺序通知候补，30 分钟内确认 */
    private void promoteWaitlist(VenueBooking b) {
        List<BookingWaitlist> list = waitlistMapper.selectList(new LambdaQueryWrapper<BookingWaitlist>()
                .eq(BookingWaitlist::getSlotId, b.getSlotId() == null ? -1 : b.getSlotId())
                .eq(BookingWaitlist::getStatus, "QUEUING")
                .orderByAsc(BookingWaitlist::getQueueNo));
        if (list.isEmpty()) {
            return;
        }
        BookingWaitlist first = list.get(0);
        first.setStatus("NOTIFIED");
        first.setNotifyTime(LocalDateTime.now());
        first.setConfirmDeadline(LocalDateTime.now().plusMinutes(30));
        waitlistMapper.updateById(first);
    }

    private void need(String from, String expect) {
        if (!expect.equals(from)) {
            throw new BizException("当前状态为 " + from + "，不允许该操作");
        }
    }

    private void needReason(String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("该操作必须填写不少于 5 个字的原因");
        }
    }

    private String actionName(String action) {
        return switch (action) {
            case "approve" -> "团体审批通过";
            case "reject" -> "审批驳回";
            case "confirm" -> "场地确认";
            case "checkin" -> "入场核验通过";
            case "leave" -> "申请离场检查";
            case "complete" -> "离场检查完成";
            case "cancel" -> "取消预约";
            default -> "未到登记";
        };
    }

    private String defaultReason(String action) {
        return switch (action) {
            case "approve" -> "人数与设备核对无误，同意使用";
            case "confirm" -> "已锁定场地与器材库存";
            case "checkin" -> "实到人数与预约一致";
            case "leave" -> "活动结束，申请离场检查";
            case "complete" -> "器材全部归还，场地完好";
            default -> "系统自动流转";
        };
    }

    private void flow(VenueBooking b, String from, String to, String action, String reason) {
        FlowLog f = new FlowLog();
        f.setBizType("BOOKING");
        f.setBizId(b.getId());
        f.setBizNo(b.getBookingNo());
        f.setFromStatus(from);
        f.setToStatus(to);
        f.setAction(action);
        f.setReason(reason);
        f.setOperator(UserContext.name());
        f.setCreateTime(LocalDateTime.now());
        flowMapper.insert(f);
    }
}
