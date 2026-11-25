package com.grad.svs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.svs.entity.BookingMember;
import org.apache.ibatis.annotations.Mapper;

/** BookingMember 数据访问 */
@Mapper
public interface BookingMemberMapper extends BaseMapper<BookingMember> {
}
