package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EquipmentBorrow（表 equipment_borrow）
 */
@Data
@TableName("equipment_borrow")
public class EquipmentBorrow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String borrowNo;

    private Long equipmentId;

    /** 关联预约 */
    private Long bookingId;

    /** 借用人 */
    private Long userId;

    /** 数量 */
    private Integer borrowCount;

    /** 交付时间 */
    private LocalDateTime borrowTime;

    /** 预计归还 */
    private LocalDateTime expectReturn;

    /** 实际归还 */
    private LocalDateTime actualReturn;

    /** RESERVED预留 BORROWED借出中 RETURNED已归还 DAMAGED损坏待赔 OVERDUE逾期 */
    private String status;

    /** 损坏说明 */
    private String damageDesc;

    /** 赔付金额 */
    private BigDecimal damageFee;

    /** 损坏照片 */
    private String damageImage;

    private LocalDateTime createTime;

}