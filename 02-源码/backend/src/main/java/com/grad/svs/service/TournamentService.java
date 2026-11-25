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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 赛事台账与赛程编排：队伍/场地/裁判同时段不重叠，保留最小休息间隔 45 分钟。 */
@Service
public class TournamentService {

    private final TournamentMapper tournamentMapper;
    private final TournamentTeamMapper teamMapper;
    private final MatchScheduleMapper matchMapper;
    private final RefereeMapper refereeMapper;
    private final SportsVenueMapper venueMapper;
    private final VenueBookingMapper bookingMapper;
    private final FlowLogMapper flowMapper;

    public TournamentService(TournamentMapper tournamentMapper, TournamentTeamMapper teamMapper,
                             MatchScheduleMapper matchMapper, RefereeMapper refereeMapper,
                             SportsVenueMapper venueMapper, VenueBookingMapper bookingMapper, FlowLogMapper flowMapper) {
        this.tournamentMapper = tournamentMapper;
        this.teamMapper = teamMapper;
        this.matchMapper = matchMapper;
        this.refereeMapper = refereeMapper;
        this.venueMapper = venueMapper;
        this.bookingMapper = bookingMapper;
        this.flowMapper = flowMapper;
    }

    public PageResult<Tournament> page(int page, int size, String status, String sportType, Long venueId) {
        LambdaQueryWrapper<Tournament> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(Tournament::getStatus, status);
        }
        if (sportType != null && !sportType.isBlank()) {
            w.eq(Tournament::getSportType, sportType);
        }
        if (venueId != null) {
            w.eq(Tournament::getVenueId, venueId);
        }
        w.orderByDesc(Tournament::getStartTime);
        Page<Tournament> p = tournamentMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public Map<String, Object> detail(Long id) {
        Tournament t = tournamentMapper.selectById(id);
        if (t == null) {
            throw new BizException("赛事不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tournament", t);
        m.put("venue", venueMapper.selectById(t.getVenueId()));
        m.put("teams", teamMapper.selectList(new LambdaQueryWrapper<TournamentTeam>()
                .eq(TournamentTeam::getTournamentId, id).orderByDesc(TournamentTeam::getPoints)));
        m.put("matches", matchMapper.selectList(new LambdaQueryWrapper<MatchSchedule>()
                .eq(MatchSchedule::getTournamentId, id).orderByAsc(MatchSchedule::getStartTime)));
        m.put("referees", refereeMapper.selectList(new LambdaQueryWrapper<Referee>().orderByAsc(Referee::getId)));
        m.put("conflicts", checkConflict(id));
        // 赛事占场对已确认普通预约的影响
        m.put("affectedBookings", bookingMapper.selectList(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, t.getVenueId())
                .in(VenueBooking::getStatus, "PENDING_CONFIRM", "PENDING_ENTRY", "IN_USE")
                .lt(VenueBooking::getStartTime, t.getEndTime())
                .gt(VenueBooking::getEndTime, t.getStartTime())));
        return m;
    }

    @Transactional
    public Tournament save(Tournament t) {
        if (t.getTournamentName() == null || t.getTournamentName().isBlank()) {
            throw new BizException("赛事名称必填");
        }
        if (t.getId() == null) {
            t.setTournamentNo("EV" + LocalDate.now().toString().replace("-", "")
                    + String.format("%03d", new Random().nextInt(999)));
            t.setStatus(t.getStatus() == null ? "DRAFT" : t.getStatus());
            t.setCreateTime(LocalDateTime.now());
            t.setUpdateTime(LocalDateTime.now());
            tournamentMapper.insert(t);
        } else {
            t.setUpdateTime(LocalDateTime.now());
            tournamentMapper.updateById(t);
        }
        return t;
    }

    /** 赛程冲突检测：场地、裁判、队伍同时段重叠 + 最小休息间隔 */
    public List<Map<String, Object>> checkConflict(Long tournamentId) {
        List<MatchSchedule> matches = matchMapper.selectList(new LambdaQueryWrapper<MatchSchedule>()
                .eq(MatchSchedule::getTournamentId, tournamentId).orderByAsc(MatchSchedule::getStartTime));
        List<Map<String, Object>> issues = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            MatchSchedule a = matches.get(i);
            if (a.getRefereeId() == null) {
                issues.add(issue("裁判未分配", a.getMatchNo(), "R" + a.getId(), "请为该场次指定主裁"));
            }
            for (int j = i + 1; j < matches.size(); j++) {
                MatchSchedule b = matches.get(j);
                boolean overlap = a.getStartTime().isBefore(b.getEndTime()) && a.getEndTime().isAfter(b.getStartTime());
                if (overlap && Objects.equals(a.getVenueId(), b.getVenueId())) {
                    issues.add(issue("场地冲突", a.getMatchNo() + " / " + b.getMatchNo(),
                            a.getStartTime().toString(), "同一场地时段重叠"));
                }
                if (overlap && a.getRefereeId() != null && a.getRefereeId().equals(b.getRefereeId())) {
                    issues.add(issue("裁判冲突", a.getMatchNo() + " / " + b.getMatchNo(),
                            a.getStartTime().toString(), "同一裁判同时段重复执裁"));
                }
                long minutes = java.time.Duration.between(a.getEndTime(), b.getStartTime()).toMinutes();
                if (minutes >= 0 && minutes < 45
                        && (Objects.equals(a.getHomeTeam(), b.getHomeTeam()) || Objects.equals(a.getHomeTeam(), b.getAwayTeam())
                        || Objects.equals(a.getAwayTeam(), b.getHomeTeam()) || Objects.equals(a.getAwayTeam(), b.getAwayTeam()))) {
                    issues.add(issue("休息间隔不足", a.getMatchNo() + " / " + b.getMatchNo(),
                            minutes + " 分钟", "同一队伍两场之间需≥45 分钟"));
                }
            }
        }
        return issues;
    }

    /** 甘特数据：按日期返回各场地场次 */
    public Map<String, Object> gantt(String date) {
        LocalDate d = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<MatchSchedule> list = matchMapper.selectList(new LambdaQueryWrapper<MatchSchedule>()
                .ge(MatchSchedule::getStartTime, start).lt(MatchSchedule::getStartTime, end)
                .orderByAsc(MatchSchedule::getStartTime));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", d.toString());
        m.put("matches", list);
        m.put("venues", venueMapper.selectList(new LambdaQueryWrapper<SportsVenue>().orderByAsc(SportsVenue::getVenueNo)));
        m.put("referees", refereeMapper.selectList(new LambdaQueryWrapper<Referee>().orderByAsc(Referee::getId)));
        return m;
    }

    /** 录入成绩并推进赛事状态 */
    @Transactional
    public MatchSchedule score(Long matchId, Integer home, Integer away) {
        MatchSchedule m = matchMapper.selectById(matchId);
        if (m == null) {
            throw new BizException("场次不存在");
        }
        if (home == null || away == null || home < 0 || away < 0) {
            throw new BizException("比分必须为非负整数");
        }
        m.setScoreHome(home);
        m.setScoreAway(away);
        m.setStatus("FINISHED");
        matchMapper.updateById(m);
        FlowLog f = new FlowLog();
        f.setBizType("TOURNAMENT");
        f.setBizId(m.getTournamentId());
        f.setBizNo(m.getMatchNo());
        f.setAction("录入成绩");
        f.setReason("比分 " + home + ":" + away);
        f.setOperator(UserContext.name());
        f.setCreateTime(LocalDateTime.now());
        flowMapper.insert(f);
        return m;
    }

    private Map<String, Object> issue(String type, String matchNo, String time, String advice) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("matchNo", matchNo);
        m.put("time", time);
        m.put("advice", advice);
        return m;
    }
}
