package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysOrg（表 sys_org）
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组织名称 */
    private String orgName;

    /** 上级组织 */
    private Long parentId;

    /** 排序 */
    private Integer sort;

    /** 1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

}