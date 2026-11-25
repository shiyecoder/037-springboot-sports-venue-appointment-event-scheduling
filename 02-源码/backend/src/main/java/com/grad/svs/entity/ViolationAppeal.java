package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ViolationAppeal（表 violation_appeal）
 */
@Data
@TableName("violation_appeal")
public class ViolationAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appealNo;

    private Long violationId;

    private Long userId;

    /** 申诉理由 */
    private String reason;

    /** 证明材料 */
    private String files;

    /** PENDING待处理 PASSED通过 REJECTED驳回 */
    private String status;

    private String handleUser;

    private LocalDateTime handleTime;

    private String handleRemark;

    private LocalDateTime createTime;

}