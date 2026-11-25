package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * VenueMaintenance（表 venue_maintenance）
 */
@Data
@TableName("venue_maintenance")
public class VenueMaintenance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String maintenanceNo;

    private Long venueId;

    /** 保养/故障/检修/改造 */
    private String maintenanceType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 整段禁用/局部禁用 */
    private String scope;

    private String owner;

    /** PENDING待处理 PROCESSING处理中 FINISHED已完成 */
    private String status;

    /** 受影响预约说明 */
    private String affectDesc;

    private LocalDateTime createTime;

}