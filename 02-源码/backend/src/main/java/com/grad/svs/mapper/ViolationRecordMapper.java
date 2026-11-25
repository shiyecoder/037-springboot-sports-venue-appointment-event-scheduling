package com.grad.svs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.svs.entity.ViolationRecord;
import org.apache.ibatis.annotations.Mapper;

/** ViolationRecord 数据访问 */
@Mapper
public interface ViolationRecordMapper extends BaseMapper<ViolationRecord> {
}
