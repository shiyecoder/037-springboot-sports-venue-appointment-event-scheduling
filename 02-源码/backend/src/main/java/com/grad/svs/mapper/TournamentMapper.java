package com.grad.svs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.svs.entity.Tournament;
import org.apache.ibatis.annotations.Mapper;

/** Tournament 数据访问 */
@Mapper
public interface TournamentMapper extends BaseMapper<Tournament> {
}
