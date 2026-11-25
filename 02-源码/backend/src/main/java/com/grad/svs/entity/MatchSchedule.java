package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * MatchSchedule（表 match_schedule）
 */
@Data
@TableName("match_schedule")
public class MatchSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tournamentId;

    /** 场次号 */
    private String matchNo;

    /** 轮次 */
    private String roundName;

    /** 主队 */
    private String homeTeam;

    /** 客队 */
    private String awayTeam;

    /** 场地 */
    private Long venueId;

    /** 裁判 */
    private Long refereeId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 主队得分 */
    private Integer scoreHome;

    /** 客队得分 */
    private Integer scoreAway;

    /** PENDING待开始 ONGOING进行中 FINISHED已结束 */
    private String status;

    private String remark;

    private LocalDateTime createTime;

}