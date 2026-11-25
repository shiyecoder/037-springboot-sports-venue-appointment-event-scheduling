package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VenueBooking（表 venue_booking）
 */
@Data
@TableName("venue_booking")
public class VenueBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约编号 */
    private String bookingNo;

    /** 场馆 */
    private Long venueId;

    /** 时段 */
    private Long slotId;

    /** PERSONAL个人 TEAM团体 */
    private String bookingType;

    /** 申请人 */
    private Long userId;

    /** 使用方/团体名称 */
    private String teamName;

    /** 联系电话 */
    private String contactPhone;

    /** 事由 */
    private String purpose;

    /** 参与人数 */
    private Integer memberCount;

    /** 场地容量上限 */
    private Integer capacityLimit;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** PENDING_APPROVAL待审批/PENDING_CONFIRM待确认/PENDING_ENTRY待入场/IN_USE使用中/LEAVING待离场检查/COMPLETED已完成/CANCELLED已取消/VIOLATED违约 */
    private String status;

    /** 预约码 */
    private String bookingCode;

    /** 费用 */
    private BigDecimal feeAmount;

    /** PAID已缴 UNPAID未缴 */
    private String feeStatus;

    /** 预留设备说明 */
    private String equipmentDesc;

    /** 安全责任书 */
    private String safetyFile;

    /** 成员名单 */
    private String memberFile;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;

    @TableLogic
    private Integer deleted;

}