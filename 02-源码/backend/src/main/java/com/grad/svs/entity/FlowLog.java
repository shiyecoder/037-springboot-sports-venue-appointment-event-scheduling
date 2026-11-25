package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * FlowLog（表 flow_log）
 */
@Data
@TableName("flow_log")
public class FlowLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String fromStatus;

    private String toStatus;

    /** 动作 */
    private String action;

    /** 原因/备注（≥5 字） */
    private String reason;

    private String operator;

    private LocalDateTime createTime;

}