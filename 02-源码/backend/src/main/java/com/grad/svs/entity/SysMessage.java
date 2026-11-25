package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysMessage（表 sys_message）
 */
@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    /** 消息类型 */
    private String msgType;

    private String title;

    private String content;

    /** 业务编号 */
    private String bizNo;

    /** 业务主键 */
    private Long bizId;

    /** 业务类型 */
    private String bizType;

    /** 1已读 */
    private Integer isRead;

    private LocalDateTime createTime;

}