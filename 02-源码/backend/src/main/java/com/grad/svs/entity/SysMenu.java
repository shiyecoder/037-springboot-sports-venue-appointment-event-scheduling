package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysMenu（表 sys_menu）
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级菜单 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** M目录 C菜单 B按钮 */
    private String menuType;

    /** 路由 */
    private String path;

    /** 权限标识 */
    private String perm;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 1显示 0隐藏 */
    private Integer visible;

    private LocalDateTime createTime;

}