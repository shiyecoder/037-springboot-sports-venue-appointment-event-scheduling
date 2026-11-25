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

/** 场馆档案：列表筛选、详情聚合（时段/预约/维护/附件）、保存与状态流转（版本号校验）。 */
@Service
public class VenueService {

    private final SportsVenueMapper venueMapper;
    private final VenueSlotMapper slotMapper;
    private final VenueBookingMapper bookingMapper;
    private final VenueMaintenanceMapper maintenanceMapper;
    private final VenueAttachmentMapper attachmentMapper;

    public VenueService(SportsVenueMapper venueMapper, VenueSlotMapper slotMapper, VenueBookingMapper bookingMapper,
                        VenueMaintenanceMapper maintenanceMapper, VenueAttachmentMapper attachmentMapper) {
        this.venueMapper = venueMapper;
        this.slotMapper = slotMapper;
        this.bookingMapper = bookingMapper;
        this.maintenanceMapper = maintenanceMapper;
        this.attachmentMapper = attachmentMapper;
    }

    public PageResult<SportsVenue> page(int page, int size, String sportType, String area, String status, String keyword) {
        LambdaQueryWrapper<SportsVenue> w = new LambdaQueryWrapper<>();
        if (sportType != null && !sportType.isBlank()) {
            w.eq(SportsVenue::getSportType, sportType);
        }
        if (area != null && !area.isBlank()) {
            w.like(SportsVenue::getArea, area);
        }
        if (status != null && !status.isBlank()) {
            w.eq(SportsVenue::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(SportsVenue::getVenueName, keyword).or().like(SportsVenue::getVenueNo, keyword));
        }
        w.orderByAsc(SportsVenue::getVenueNo);
        Page<SportsVenue> p = venueMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public List<SportsVenue> options() {
        return venueMapper.selectList(new LambdaQueryWrapper<SportsVenue>()
                .in(SportsVenue::getStatus, "NORMAL", "MAINTENANCE").orderByAsc(SportsVenue::getVenueNo));
    }

    /** 详情：基础信息 + 未来 7 天时段 + 近期预约 + 维护记录 + 附件 */
    public Map<String, Object> detail(Long id) {
        SportsVenue v = venueMapper.selectById(id);
        if (v == null) {
            throw new BizException("场馆不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("venue", v);
        m.put("slots", slotMapper.selectList(new LambdaQueryWrapper<VenueSlot>()
                .eq(VenueSlot::getVenueId, id).orderByAsc(VenueSlot::getSlotDate).orderByAsc(VenueSlot::getStartTime)));
        m.put("bookings", bookingMapper.selectList(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, id).orderByDesc(VenueBooking::getStartTime).last("limit 10")));
        m.put("maintenances", maintenanceMapper.selectList(new LambdaQueryWrapper<VenueMaintenance>()
                .eq(VenueMaintenance::getVenueId, id).orderByDesc(VenueMaintenance::getStartTime)));
        m.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<VenueAttachment>()
                .eq(VenueAttachment::getBizType, "VENUE").eq(VenueAttachment::getBizId, id)));
        return m;
    }

    @Transactional
    public SportsVenue save(SportsVenue v) {
        v.setVenueNo(v.getVenueNo() == null ? null : v.getVenueNo().trim());
        if (v.getVenueNo() == null || v.getVenueNo().isBlank()) {
            throw new BizException("场馆编号必填");
        }
        if (v.getVenueNo().length() > 50) {
            throw new BizException("场馆编号长度不能超过 50 个字符");
        }
        if (v.getVenueName() == null || v.getVenueName().isBlank()) {
            throw new BizException("场馆名称必填");
        }
        if (v.getCapacity() != null && v.getCapacity() < 0) {
            throw new BizException("容纳人数不能为负数");
        }
        LambdaQueryWrapper<SportsVenue> dup = new LambdaQueryWrapper<SportsVenue>()
                .eq(SportsVenue::getVenueNo, v.getVenueNo());
        if (v.getId() != null) {
            dup.ne(SportsVenue::getId, v.getId());
        }
        if (venueMapper.selectCount(dup) > 0) {
            throw new BizException("场馆编号已存在：" + v.getVenueNo());
        }
        if (v.getId() == null) {
            v.setStatus(v.getStatus() == null ? "NORMAL" : v.getStatus());
            v.setCreateBy(UserContext.name());
            v.setCreateTime(LocalDateTime.now());
            v.setVersion(0);
            venueMapper.insert(v);
        } else {
            SportsVenue old = venueMapper.selectById(v.getId());
            if (old == null) {
                throw new BizException("场馆不存在");
            }
            if (v.getVersion() != null && !v.getVersion().equals(old.getVersion())) {
                throw new BizException("该场馆已被他人修改，请刷新后重试");
            }
            v.setVersion(old.getVersion() + 1);
            v.setUpdateTime(LocalDateTime.now());
            venueMapper.updateById(v);
        }
        return v;
    }

    /** 状态流转：启用 / 维护 / 停用；停用前检查是否有未完成预约 */
    @Transactional
    public SportsVenue transition(Long id, String target, String reason) {
        SportsVenue v = venueMapper.selectById(id);
        if (v == null) {
            throw new BizException("场馆不存在");
        }
        if (!List.of("NORMAL", "MAINTENANCE", "DISABLED").contains(target)) {
            throw new BizException("不支持的目标状态：" + target);
        }
        if ("DISABLED".equals(target)) {
            long active = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>()
                    .eq(VenueBooking::getVenueId, id)
                    .in(VenueBooking::getStatus, "PENDING_APPROVAL", "PENDING_CONFIRM", "PENDING_ENTRY", "IN_USE", "LEAVING"));
            if (active > 0) {
                throw new BizException("该场馆还有 " + active + " 条未完成预约，不能直接停用，请先协调换场或取消");
            }
        }
        if ("MAINTENANCE".equals(target) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("转入维护必须填写不少于 5 个字的原因");
        }
        v.setStatus(target);
        v.setVersion(v.getVersion() == null ? 1 : v.getVersion() + 1);
        v.setUpdateTime(LocalDateTime.now());
        venueMapper.updateById(v);
        return v;
    }
}
