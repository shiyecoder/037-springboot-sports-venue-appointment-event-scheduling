package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * VenueAttachment（表 venue_attachment）
 */
@Data
@TableName("venue_attachment")
public class VenueAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** VENUE场馆 BOOKING预约 TOURNAMENT赛事 */
    private String bizType;

    private Long bizId;

    private String fileName;

    private String path;

    private Long fileSize;

    private String uploader;

    private LocalDateTime createTime;

}