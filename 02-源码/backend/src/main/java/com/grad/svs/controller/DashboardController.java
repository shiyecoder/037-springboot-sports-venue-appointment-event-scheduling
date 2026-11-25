package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.entity.VenueSlot;
import com.grad.svs.mapper.VenueSlotMapper;
import com.grad.svs.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/** 场馆经营看板 + 预约周历 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final VenueSlotMapper slotMapper;

    public DashboardController(DashboardService dashboardService, VenueSlotMapper slotMapper) {
        this.dashboardService = dashboardService;
        this.slotMapper = slotMapper;
    }

    @GetMapping("/dashboard")
    public R<Object> dashboard() {
        return R.ok(dashboardService.overview());
    }

    /** 预约周历：按场馆与周次返回时段占用 */
    @GetMapping("/calendar")
    public R<Object> calendar(@RequestParam(required = false) Long venueId,
                              @RequestParam(required = false) String start,
                              @RequestParam(required = false) String end) {
        LocalDate from = start == null || start.isBlank() ? LocalDate.now() : LocalDate.parse(start);
        LocalDate to = end == null || end.isBlank() ? from.plusDays(6) : LocalDate.parse(end);
        LambdaQueryWrapper<VenueSlot> w = new LambdaQueryWrapper<VenueSlot>()
                .ge(VenueSlot::getSlotDate, from).le(VenueSlot::getSlotDate, to)
                .orderByAsc(VenueSlot::getSlotDate).orderByAsc(VenueSlot::getStartTime);
        if (venueId != null) {
            w.eq(VenueSlot::getVenueId, venueId);
        }
        return R.ok(slotMapper.selectList(w));
    }
}
