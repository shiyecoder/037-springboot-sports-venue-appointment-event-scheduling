package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * VenueSlot（表 venue_slot）
 */
@Data
@TableName("venue_slot")
public class VenueSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场馆 */
    private Long venueId;

    /** 日期 */
    private LocalDate slotDate;

    /** 开始 HH:mm */
    private String startTime;

    /** 结束 HH:mm */
    private String endTime;

    /** 人数/场次容量 */
    private Integer capacity;

    /** 已锁定名额 */
    private Integer bookedCount;

    /** OPEN可约 FULL满额 DISABLED禁用 */
    private String status;

    private String remark;

    private LocalDateTime createTime;

}