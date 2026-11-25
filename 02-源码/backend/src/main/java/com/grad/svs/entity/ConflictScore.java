package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ConflictScore（表 conflict_score）
 */
@Data
@TableName("conflict_score")
public class ConflictScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单据编号 */
    private String bizNo;

    /** BOOKING预约 TOURNAMENT赛事 */
    private String bizType;

    private Long ruleId;

    private String ruleVersion;

    /** 输入快照JSON */
    private String inputSnapshot;

    /** 总分 */
    private BigDecimal totalScore;

    /** RECOMMEND推荐可用 COORDINATE需人工协调 RESCHEDULE建议改期 */
    private String grade;

    /** 各因素明细JSON */
    private String factorJson;

    /** 缺失项 */
    private String missingJson;

    /** 可改进项 */
    private String improveJson;

    /** 计算时间 */
    private LocalDateTime calcTime;

    /** 1已人工调整 */
    private Integer adjusted;

    /** 调整后得分 */
    private BigDecimal adjustScore;

    private String adjustReason;

    private String adjustUser;

    private LocalDateTime adjustTime;

    private LocalDateTime createTime;

}