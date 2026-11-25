package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ViolationRecord（表 violation_record）
 */
@Data
@TableName("violation_record")
public class ViolationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String violationNo;

    private Long userId;

    private String userName;

    /** 无故未到/超时占用/违规使用/设备损坏 */
    private String violationType;

    /** 关联单据 */
    private String bizNo;

    private LocalDate occurDate;

    /** 处理结果 */
    private String punish;

    /** 诚信分变化 */
    private Integer creditDelta;

    /** EFFECTIVE已生效 APPEALING申诉中 REVOKED已撤销 */
    private String status;

    private LocalDateTime createTime;

}