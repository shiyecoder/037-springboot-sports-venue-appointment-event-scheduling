package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.entity.VenueBooking;
import com.grad.svs.service.BookingService;
import com.grad.svs.service.ConflictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 预约、核验与冲突协同评估 */
@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;
    private final ConflictService conflictService;

    public BookingController(BookingService bookingService, ConflictService conflictService) {
        this.bookingService = bookingService;
        this.conflictService = conflictService;
    }

    @GetMapping("/page")
    public R<Object> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status, @RequestParam(required = false) String sportType,
                          @RequestParam(required = false) Long venueId, @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String tab) {
        return R.ok(bookingService.page(page, size, status, sportType, venueId, keyword, tab));
    }

    @GetMapping("/{id}")
    public R<Object> detail(@PathVariable Long id) {
        return R.ok(bookingService.detail(id));
    }

    @PostMapping
    public R<VenueBooking> create(@RequestBody VenueBooking booking) {
        booking.setId(null);
        return R.ok(bookingService.create(booking));
    }

    @PostMapping("/{id}/transition")
    public R<VenueBooking> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = String.valueOf(body.get("action"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        Integer actual = body.get("actualCount") == null ? null : Integer.valueOf(String.valueOf(body.get("actualCount")));
        return R.ok(bookingService.transition(id, action, reason, actual));
    }

    /** 按预约码核验 */
    @GetMapping("/verify")
    public R<Object> verify(@RequestParam String code) {
        return R.ok(bookingService.verify(code));
    }

    /** 执行创新点评估并返回明细 */
    @PostMapping("/{id}/evaluate")
    public R<Object> evaluate(@PathVariable Long id) {
        Object score = conflictService.evaluate(id);
        return R.ok(score);
    }

    /** 冲突协同计算留痕 */
    @GetMapping("/traces")
    public R<List<Object>> traces(@RequestParam(required = false) String bizNo,
                                 @RequestParam(defaultValue = "20") int limit) {
        return R.ok(List.copyOf(conflictService.traces(bizNo, limit)));
    }
}
