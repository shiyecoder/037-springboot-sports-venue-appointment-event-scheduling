package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysLog（表 sys_log）
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    /** 模块 */
    private String module;

    /** 动作 */
    private String action;

    /** 详情 */
    private String detail;

    private String ip;

    private String result;

    private LocalDateTime createTime;

}