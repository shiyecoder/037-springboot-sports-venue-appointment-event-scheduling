package com.grad.svs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.svs.common.BizException;
import com.grad.svs.common.PageResult;
import com.grad.svs.common.R;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import com.grad.svs.service.ConflictService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 候补、设备借用、场地维护、违约申诉、规则配置 */
@RestController
@RequestMapping("/api")
public class MiscController {

    private final BookingWaitlistMapper waitlistMapper;
    private final SportsVenueMapper venueMapper;
    private final VenueSlotMapper slotMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentBorrowMapper borrowMapper;
    private final VenueMaintenanceMapper maintenanceMapper;
    private final VenueBookingMapper bookingMapper;
    private final ViolationRecordMapper violationMapper;
    private final ViolationAppealMapper appealMapper;
    private final ConflictService conflictService;

    public MiscController(BookingWaitlistMapper waitlistMapper, SportsVenueMapper venueMapper,
                          VenueSlotMapper slotMapper, EquipmentMapper equipmentMapper,
                          EquipmentBorrowMapper borrowMapper, VenueMaintenanceMapper maintenanceMapper,
                          VenueBookingMapper bookingMapper, ViolationRecordMapper violationMapper,
                          ViolationAppealMapper appealMapper, ConflictService conflictService) {
        this.waitlistMapper = waitlistMapper;
        this.venueMapper = venueMapper;
        this.slotMapper = slotMapper;
        this.equipmentMapper = equipmentMapper;
        this.borrowMapper = borrowMapper;
        this.maintenanceMapper = maintenanceMapper;
        this.bookingMapper = bookingMapper;
        this.violationMapper = violationMapper;
        this.appealMapper = appealMapper;
        this.conflictService = conflictService;
    }

    // ---------------- 候补
    @GetMapping("/waitlist/page")
    public R<Object> waitlistPage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BookingWaitlist> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(BookingWaitlist::getStatus, status);
        }
        w.orderByAsc(BookingWaitlist::getQueueNo).orderByDesc(BookingWaitlist::getId);
        Page<BookingWaitlist> p = waitlistMapper.selectPage(new Page<>(page, size), w);
        return R.ok(new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 候补转正确认：30 分钟内有效 */
    @PostMapping("/waitlist/{id}/confirm")
    public R<BookingWaitlist> confirmWaitlist(@PathVariable Long id) {
        BookingWaitlist wl = waitlistMapper.selectById(id);
        if (wl == null) {
            throw new BizException("候补记录不存在");
        }
        if (!"NOTIFIED".equals(wl.getStatus())) {
            throw new BizException("当前候补状态为 " + wl.getStatus() + "，不能确认转正");
        }
        if (wl.getConfirmDeadline() != null && wl.getConfirmDeadline().isBefore(LocalDateTime.now())) {
            wl.setStatus("EXPIRED");
            waitlistMapper.updateById(wl);
            throw new BizException("已超过确认截止时间，候补名额顺延至下一位");
        }
        wl.setStatus("CONFIRMED");
        waitlistMapper.updateById(wl);
        return R.ok(wl);
    }

    // ---------------- 设备
    @GetMapping("/equipment/page")
    public R<List<Equipment>> equipmentList() {
        return R.ok(equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>().orderByAsc(Equipment::getEquipmentNo)));
    }

    @GetMapping("/equipment/borrow/page")
    public R<Object> borrowPage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String status) {
        LambdaQueryWrapper<EquipmentBorrow> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(EquipmentBorrow::getStatus, status);
        }
        w.orderByDesc(EquipmentBorrow::getBorrowTime);
        Page<EquipmentBorrow> p = borrowMapper.selectPage(new Page<>(page, size), w);
        return R.ok(new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 离场检查：归还或登记损坏（损坏自动生成违约记录并扣减可借数） */
    @PostMapping("/equipment/borrow/{id}/return")
    @Transactional
    public R<EquipmentBorrow> returnEquipment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EquipmentBorrow br = borrowMapper.selectById(id);
        if (br == null) {
            throw new BizException("借用记录不存在");
        }
        String damage = body.get("damageDesc") == null ? null : String.valueOf(body.get("damageDesc"));
        BigDecimal fee = body.get("damageFee") == null ? BigDecimal.ZERO
                : new BigDecimal(String.valueOf(body.get("damageFee")));
        br.setActualReturn(LocalDateTime.now());
        if (damage != null && !damage.isBlank()) {
            br.setStatus("DAMAGED");
            br.setDamageDesc(damage);
            br.setDamageFee(fee);
            Equipment eq = equipmentMapper.selectById(br.getEquipmentId());
            if (eq != null) {
                eq.setTotalCount(Math.max(0, eq.getTotalCount() - (br.getBorrowCount() == null ? 1 : br.getBorrowCount())));
                equipmentMapper.updateById(eq);
            }
        } else {
            br.setStatus("RETURNED");
            Equipment eq = equipmentMapper.selectById(br.getEquipmentId());
            if (eq != null) {
                eq.setAvailableCount(Math.min(eq.getTotalCount(),
                        (eq.getAvailableCount() == null ? 0 : eq.getAvailableCount()) + (br.getBorrowCount() == null ? 1 : br.getBorrowCount())));
                equipmentMapper.updateById(eq);
            }
        }
        borrowMapper.updateById(br);
        return R.ok(br);
    }

    // ---------------- 维护
    @GetMapping("/maintenance/page")
    public R<Object> maintenancePage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String status) {
        LambdaQueryWrapper<VenueMaintenance> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(VenueMaintenance::getStatus, status);
        }
        w.orderByDesc(VenueMaintenance::getStartTime);
        Page<VenueMaintenance> p = maintenanceMapper.selectPage(new Page<>(page, size), w);
        return R.ok(new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 登记维护：同步把受影响时段置为禁用，并给出受影响预约 */
    @PostMapping("/maintenance")
    @Transactional
    public R<Map<String, Object>> createMaintenance(@RequestBody VenueMaintenance mt) {
        if (mt.getVenueId() == null || mt.getStartTime() == null || mt.getEndTime() == null) {
            throw new BizException("场馆与起止时间必填");
        }
        if (mt.getMaintenanceType() == null || mt.getMaintenanceType().isBlank()) {
            throw new BizException("维护类型必填");
        }
        mt.setMaintenanceNo("MT" + LocalDate.now().toString().replace("-", "")
                + String.format("%03d", new Random().nextInt(999)));
        mt.setStatus("PENDING");
        mt.setCreateTime(LocalDateTime.now());
        maintenanceMapper.insert(mt);
        List<VenueSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<VenueSlot>()
                .eq(VenueSlot::getVenueId, mt.getVenueId())
                .ge(VenueSlot::getSlotDate, mt.getStartTime().toLocalDate())
                .le(VenueSlot::getSlotDate, mt.getEndTime().toLocalDate()));
        for (VenueSlot s : slots) {
            s.setStatus("DISABLED");
            s.setRemark("维护禁用：" + mt.getMaintenanceNo());
            slotMapper.updateById(s);
        }
        List<VenueBooking> affected = bookingMapper.selectList(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, mt.getVenueId())
                .in(VenueBooking::getStatus, "PENDING_CONFIRM", "PENDING_ENTRY", "PENDING_APPROVAL")
                .lt(VenueBooking::getStartTime, mt.getEndTime())
                .gt(VenueBooking::getEndTime, mt.getStartTime()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maintenance", mt);
        m.put("affected", affected);
        m.put("disabledSlots", slots.size());
        return R.ok(m);
    }

    // ---------------- 违约与申诉
    @GetMapping("/violation/page")
    public R<Object> violationPage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ViolationRecord> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(ViolationRecord::getStatus, status);
        }
        w.orderByDesc(ViolationRecord::getOccurDate);
        Page<ViolationRecord> p = violationMapper.selectPage(new Page<>(page, size), w);
        return R.ok(new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/violation/appeal/page")
    public R<List<ViolationAppeal>> appealList(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<ViolationAppeal> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            w.eq(ViolationAppeal::getStatus, status);
        }
        w.orderByDesc(ViolationAppeal::getId);
        return R.ok(appealMapper.selectList(w));
    }

    /** 申诉处理：通过则撤销对应违约记录 */
    @PostMapping("/violation/appeal/{id}/handle")
    @Transactional
    public R<ViolationAppeal> handleAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ViolationAppeal ap = appealMapper.selectById(id);
        if (ap == null) {
            throw new BizException("申诉不存在");
        }
        if (!"PENDING".equals(ap.getStatus())) {
            throw new BizException("该申诉已处理，不能重复处理");
        }
        String pass = body.getOrDefault("pass", "false");
        String remark = body.get("remark");
        if (remark == null || remark.trim().length() < 5) {
            throw new BizException("处理意见必须填写不少于 5 个字");
        }
        ap.setStatus(Boolean.parseBoolean(pass) ? "PASSED" : "REJECTED");
        ap.setHandleUser(UserContext.name());
        ap.setHandleTime(LocalDateTime.now());
        ap.setHandleRemark(remark.trim());
        appealMapper.updateById(ap);
        if ("PASSED".equals(ap.getStatus())) {
            ViolationRecord vr = violationMapper.selectById(ap.getViolationId());
            if (vr != null) {
                vr.setStatus("REVOKED");
                violationMapper.updateById(vr);
            }
        }
        return R.ok(ap);
    }

    // ---------------- 规则配置
    @GetMapping("/rule/current")
    public R<Object> currentRule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rule", conflictService.activeRule());
        m.put("history", conflictService.rules());
        return R.ok(m);
    }

    @PostMapping("/rule")
    public R<ConflictRule> saveRule(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> weights = (Map<String, Integer>) body.get("weights");
        @SuppressWarnings("unchecked")
        Map<String, Integer> thresholds = (Map<String, Integer>) body.get("thresholds");
        return R.ok(conflictService.saveRule(String.valueOf(body.get("name")),
                String.valueOf(body.get("version")), weights,
                thresholds == null ? Map.of("recommend", 80, "coordinate", 60) : thresholds));
    }

    @PostMapping("/rule/adjust/{scoreId}")
    public R<Object> adjust(@PathVariable Long scoreId, @RequestBody Map<String, String> body) {
        return R.ok(conflictService.adjust(scoreId, body.get("grade"), body.get("reason")));
    }
}
