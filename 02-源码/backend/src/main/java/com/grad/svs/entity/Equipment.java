package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Equipment（表 equipment）
 */
@Data
@TableName("equipment")
public class Equipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 器材编号 */
    private String equipmentNo;

    /** 器材名称 */
    private String equipmentName;

    /** 分类 */
    private String equipmentType;

    /** 总数 */
    private Integer totalCount;

    /** 可借数 */
    private Integer availableCount;

    /** 存放场馆 */
    private Long venueId;

    /** NORMAL/SCRAPPED */
    private String status;

    private LocalDateTime createTime;

}