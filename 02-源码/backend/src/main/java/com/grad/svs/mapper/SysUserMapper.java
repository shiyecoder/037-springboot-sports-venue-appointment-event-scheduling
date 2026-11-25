package com.grad.svs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.svs.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** SysUser 数据访问 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
