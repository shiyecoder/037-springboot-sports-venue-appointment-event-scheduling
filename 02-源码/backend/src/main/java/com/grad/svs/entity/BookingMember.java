package com.grad.svs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * BookingMember（表 booking_member）
 */
@Data
@TableName("booking_member")
public class BookingMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private String memberName;

    private String memberPhone;

    /** 证件后四位 */
    private String idCardTail;

    private LocalDateTime createTime;

}