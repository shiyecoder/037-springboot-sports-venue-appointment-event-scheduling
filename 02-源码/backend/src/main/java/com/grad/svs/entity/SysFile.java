package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysFile（表 sys_file）
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 字节 */
    private Long fileSize;

    /** 扩展名 */
    private String fileType;

    /** 相对路径 */
    private String path;

    /** 业务类型 */
    private String bizType;

    private String uploader;

    private LocalDateTime createTime;

}