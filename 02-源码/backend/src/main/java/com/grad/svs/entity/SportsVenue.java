package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SportsVenue（表 sports_venue）
 */
@Data
@TableName("sports_venue")
public class SportsVenue {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场馆编号 */
    private String venueNo;

    /** 场馆名称 */
    private String venueName;

    /** 运动项目 */
    private String sportType;

    /** 所在区域 */
    private String area;

    /** 容纳人数 */
    private Integer capacity;

    /** 地面类型 */
    private String floorType;

    /** 配套设备 */
    private String equipment;

    /** 开放时段 */
    private String openTime;

    /** 校外收费标准(元/小时) */
    private BigDecimal priceHour;

    /** 校内收费标准 */
    private BigDecimal priceStudent;

    /** NORMAL启用 MAINTENANCE维护中 DISABLED停用 */
    private String status;

    /** 场馆图片 */
    private String coverImage;

    /** 图片2 */
    private String image2;

    /** 图片3 */
    private String image3;

    /** 责任单位 */
    private String manager;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 版本号（乐观锁） */
    private Integer version;

    @TableLogic
    private Integer deleted;

}