package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.entity.SportsVenue;
import com.grad.svs.entity.VenueSlot;
import com.grad.svs.mapper.VenueSlotMapper;
import com.grad.svs.service.VenueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 场馆档案与时段 */
@RestController
@RequestMapping("/api/venue")
public class VenueController {

    private final VenueService venueService;
    private final VenueSlotMapper slotMapper;

    public VenueController(VenueService venueService, VenueSlotMapper slotMapper) {
        this.venueService = venueService;
        this.slotMapper = slotMapper;
    }

    @GetMapping("/page")
    public R<Object> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String sportType, @RequestParam(required = false) String area,
                          @RequestParam(required = false) String status, @RequestParam(required = false) String keyword) {
        return R.ok(venueService.page(page, size, sportType, area, status, keyword));
    }

    @GetMapping("/options")
    public R<List<SportsVenue>> options() {
        return R.ok(venueService.options());
    }

    @GetMapping("/{id}")
    public R<Object> detail(@PathVariable Long id) {
        return R.ok(venueService.detail(id));
    }

    @PostMapping
    public R<SportsVenue> create(@RequestBody SportsVenue venue) {
        venue.setId(null);
        return R.ok(venueService.save(venue));
    }

    @PutMapping("/{id}")
    public R<SportsVenue> update(@PathVariable Long id, @RequestBody SportsVenue venue) {
        venue.setId(id);
        return R.ok(venueService.save(venue));
    }

    /** 状态流转：NORMAL / MAINTENANCE / DISABLED */
    @PostMapping("/{id}/transition")
    public R<SportsVenue> transition(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(venueService.transition(id, body.get("target"), body.get("reason")));
    }

    /** 某场馆某日期的可约时段 */
    @GetMapping("/{id}/slots")
    public R<List<VenueSlot>> slots(@PathVariable Long id, @RequestParam(required = false) String date) {
        LambdaQueryWrapper<VenueSlot> w = new LambdaQueryWrapper<VenueSlot>().eq(VenueSlot::getVenueId, id);
        if (date != null && !date.isBlank()) {
            w.eq(VenueSlot::getSlotDate, date);
        }
        w.orderByAsc(VenueSlot::getSlotDate).orderByAsc(VenueSlot::getStartTime);
        return R.ok(slotMapper.selectList(w));
    }
}
