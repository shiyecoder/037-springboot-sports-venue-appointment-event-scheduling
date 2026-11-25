package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ConflictRule（表 conflict_rule）
 */
@Data
@TableName("conflict_rule")
public class ConflictRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    /** 规则版本 */
    private String ruleVersion;

    /** 各因素权重JSON */
    private String weightsJson;

    /** 等级阈值JSON */
    private String thresholdsJson;

    /** ACTIVE启用 ARCHIVED归档 */
    private String status;

    /** 生效时间 */
    private LocalDateTime effectTime;

    private String createBy;

    private LocalDateTime createTime;

}