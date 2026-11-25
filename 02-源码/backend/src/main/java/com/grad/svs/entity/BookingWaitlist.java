package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * BookingWaitlist（表 booking_waitlist）
 */
@Data
@TableName("booking_waitlist")
public class BookingWaitlist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String waitlistNo;

    private Long venueId;

    private Long slotId;

    private Long userId;

    private Integer memberCount;

    /** 排队顺序 */
    private Integer queueNo;

    /** QUEUING排队中 NOTIFIED待确认 CONFIRMED已转正 EXPIRED已过期 CANCELLED已取消 */
    private String status;

    private LocalDateTime notifyTime;

    /** 确认截止 */
    private LocalDateTime confirmDeadline;

    private LocalDateTime createTime;

}