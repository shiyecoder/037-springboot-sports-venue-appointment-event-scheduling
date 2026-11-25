package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysDict（表 sys_dict）
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 字典标签 */
    private String dictLabel;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

}