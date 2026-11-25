package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Referee（表 referee）
 */
@Data
@TableName("referee")
public class Referee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refereeNo;

    private String realName;

    /** 主裁/边裁/记录员/器材保障 */
    private String roleType;

    /** 执裁项目 */
    private String sportType;

    private String phone;

    /** 可排期时段 */
    private String availableTime;

    /** 本月执法场次 */
    private Integer monthCount;

    private String status;

    private String avatar;

    private LocalDateTime createTime;

}