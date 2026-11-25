package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * TournamentTeam（表 tournament_team）
 */
@Data
@TableName("tournament_team")
public class TournamentTeam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tournamentId;

    private String teamName;

    private String leader;

    private Integer memberCount;

    /** 已赛场次 */
    private Integer played;

    /** 积分 */
    private Integer points;

    private LocalDateTime createTime;

}