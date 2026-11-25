package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Tournament（表 tournament）
 */
@Data
@TableName("tournament")
public class Tournament {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tournamentNo;

    private String tournamentName;

    private String sportType;

    /** 主场地 */
    private Long venueId;

    /** 赛事级别 */
    private String level;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer teamCount;

    private Integer matchCount;

    /** 优先级 1普通 2重要 3最高（赛事占场） */
    private Integer priority;

    /** 主裁 */
    private Long refereeId;

    /** 负责人 */
    private Long ownerId;

    /** DRAFT草稿 SCHEDULED已排期 ONGOING进行中 FINISHED已结束 CANCELLED已取消 */
    private String status;

    /** 赛事海报 */
    private String poster;

    /** 安全材料 */
    private String safetyFile;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}