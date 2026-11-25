package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 场馆经营看板：6 个核心指标 + 6 个图表，全部由后端聚合，前端不二次全表计算。 */
@Service
public class DashboardService {

    private final VenueBookingMapper bookingMapper;
    private final SportsVenueMapper venueMapper;
    private final VenueSlotMapper slotMapper;
    private final TournamentMapper tournamentMapper;
    private final MatchScheduleMapper matchMapper;
    private final BookingWaitlistMapper waitlistMapper;
    private final EquipmentBorrowMapper borrowMapper;
    private final ViolationRecordMapper violationMapper;
    private final ConflictScoreMapper scoreMapper;
    private final VenueMaintenanceMapper maintenanceMapper;

    public DashboardService(VenueBookingMapper bookingMapper, SportsVenueMapper venueMapper, VenueSlotMapper slotMapper,
                            TournamentMapper tournamentMapper, MatchScheduleMapper matchMapper,
                            BookingWaitlistMapper waitlistMapper, EquipmentBorrowMapper borrowMapper,
                            ViolationRecordMapper violationMapper, ConflictScoreMapper scoreMapper,
                            VenueMaintenanceMapper maintenanceMapper) {
        this.bookingMapper = bookingMapper;
        this.venueMapper = venueMapper;
        this.slotMapper = slotMapper;
        this.tournamentMapper = tournamentMapper;
        this.matchMapper = matchMapper;
        this.waitlistMapper = waitlistMapper;
        this.borrowMapper = borrowMapper;
        this.violationMapper = violationMapper;
        this.scoreMapper = scoreMapper;
        this.maintenanceMapper = maintenanceMapper;
    }

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(30).atStartOfDay();
        List<SportsVenue> venues = venueMapper.selectList(null);
        List<VenueBooking> all = bookingMapper.selectList(null);
        long totalSlots = slotMapper.selectCount(null);
        long bookedSlots = slotMapper.selectCount(new LambdaQueryWrapper<VenueSlot>()
                .gt(VenueSlot::getBookedCount, 0));

        Map<String, Object> m = new LinkedHashMap<>();
        // 1 指标
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("venueUsage", totalSlots == 0 ? 0 : Math.round(bookedSlots * 1000.0 / totalSlots) / 10.0);
        kpi.put("bookingCount", all.size());
        kpi.put("matchCount", matchMapper.selectCount(null));
        kpi.put("conflictCount", scoreMapper.selectCount(new LambdaQueryWrapper<ConflictScore>()
                .ne(ConflictScore::getGrade, "RECOMMEND")));
        long checked = all.stream().filter(b -> List.of("IN_USE", "LEAVING", "COMPLETED").contains(b.getStatus())).count();
        long shouldCheck = all.stream().filter(b -> !List.of("CANCELLED", "PENDING_APPROVAL", "PENDING_CONFIRM").contains(b.getStatus())).count();
        kpi.put("checkinRate", shouldCheck == 0 ? 0 : Math.round(checked * 1000.0 / shouldCheck) / 10.0);
        BigDecimal income = all.stream().filter(b -> "PAID".equals(b.getFeeStatus()))
                .map(b -> b.getFeeAmount() == null ? BigDecimal.ZERO : b.getFeeAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        kpi.put("income", income);
        m.put("kpi", kpi);

        // 2 趋势（最近 12 天预约量）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long c = all.stream().filter(b -> b.getStartTime() != null && b.getStartTime().toLocalDate().equals(d)).count();
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("date", d.toString().substring(5));
            pt.put("value", c);
            trend.add(pt);
        }
        m.put("trend", trend);

        // 3 状态分布
        List<Map<String, Object>> statusDist = new ArrayList<>();
        for (String st : List.of("PENDING_APPROVAL", "PENDING_CONFIRM", "PENDING_ENTRY", "IN_USE", "LEAVING", "COMPLETED", "CANCELLED", "VIOLATED")) {
            long c = all.stream().filter(b -> st.equals(b.getStatus())).count();
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", st);
            e.put("value", c);
            statusDist.add(e);
        }
        m.put("statusDist", statusDist);

        // 4 项目热度
        Map<String, Integer> heat = new LinkedHashMap<>();
        for (SportsVenue v : venues) {
            heat.put(v.getSportType(), 0);
        }
        for (VenueBooking b : all) {
            venues.stream().filter(v -> v.getId().equals(b.getVenueId())).findFirst()
                    .ifPresent(v -> heat.merge(v.getSportType(), 1, Integer::sum));
        }
        List<Map<String, Object>> hot = new ArrayList<>();
        heat.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("name", e.getKey());
            x.put("value", e.getValue());
            hot.add(x);
        });
        m.put("sportHeat", hot);

        // 5 时段热力
        Map<String, Integer> hourHeat = new LinkedHashMap<>();
        for (VenueBooking b : all) {
            if (b.getStartTime() == null) {
                continue;
            }
            String key = String.format("%02d:00", b.getStartTime().getHour());
            hourHeat.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> hours = new ArrayList<>();
        hourHeat.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("name", e.getKey());
            x.put("value", e.getValue());
            hours.add(x);
        });
        m.put("hourHeat", hours);

        // 6 冲突协同结果分布
        List<Map<String, Object>> gradeDist = new ArrayList<>();
        for (String g : List.of("RECOMMEND", "COORDINATE", "RESCHEDULE")) {
            long c = scoreMapper.selectCount(new LambdaQueryWrapper<ConflictScore>().eq(ConflictScore::getGrade, g));
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("name", g);
            x.put("value", c);
            gradeDist.add(x);
        }
        m.put("gradeDist", gradeDist);
        long adjusted = scoreMapper.selectCount(new LambdaQueryWrapper<ConflictScore>().eq(ConflictScore::getAdjusted, 1));
        long totalScore = scoreMapper.selectCount(null);
        m.put("adopt", Map.of("total", totalScore, "adjusted", adjusted,
                "adoptRate", totalScore == 0 ? 0 : Math.round((totalScore - adjusted) * 1000.0 / totalScore) / 10.0));

        // 7 场地利用率排行
        List<Map<String, Object>> usage = new ArrayList<>();
        for (SportsVenue v : venues) {
            long cnt = all.stream().filter(b -> v.getId().equals(b.getVenueId())).count();
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("name", v.getVenueName());
            x.put("value", cnt);
            x.put("capacity", v.getCapacity());
            usage.add(x);
        }
        usage.sort((a, b) -> ((Long) b.get("value")).compareTo((Long) a.get("value")));
        m.put("venueUsageRank", usage.size() > 8 ? usage.subList(0, 8) : usage);

        m.put("waitlistCount", waitlistMapper.selectCount(new LambdaQueryWrapper<BookingWaitlist>()
                .in(BookingWaitlist::getStatus, "QUEUING", "NOTIFIED")));
        m.put("damageCount", borrowMapper.selectCount(new LambdaQueryWrapper<EquipmentBorrow>()
                .in(EquipmentBorrow::getStatus, "DAMAGED", "OVERDUE")));
        m.put("violationCount", violationMapper.selectCount(new LambdaQueryWrapper<ViolationRecord>()
                .eq(ViolationRecord::getStatus, "EFFECTIVE")));
        m.put("maintenanceCount", maintenanceMapper.selectCount(new LambdaQueryWrapper<VenueMaintenance>()
                .in(VenueMaintenance::getStatus, "PENDING", "PROCESSING")));
        return m;
    }
}
