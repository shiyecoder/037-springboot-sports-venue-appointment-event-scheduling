package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.FlowLog;
import com.grad.svs.entity.SysLog;
import com.grad.svs.entity.SysMessage;
import com.grad.svs.entity.SysUser;
import com.grad.svs.mapper.FlowLogMapper;
import com.grad.svs.mapper.SysLogMapper;
import com.grad.svs.mapper.SysMessageMapper;
import com.grad.svs.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 留痕服务：业务流转时间轴、站内消息、操作日志。
 * 任何会改变业务状态的操作都必须调用 record，保证可追溯。
 */
@Service
public class LogService {

    private final FlowLogMapper flowLogMapper;
    private final SysMessageMapper messageMapper;
    private final SysLogMapper logMapper;
    private final SysUserMapper userMapper;

    public LogService(FlowLogMapper flowLogMapper, SysMessageMapper messageMapper,
                      SysLogMapper logMapper, SysUserMapper userMapper) {
        this.flowLogMapper = flowLogMapper;
        this.messageMapper = messageMapper;
        this.logMapper = logMapper;
        this.userMapper = userMapper;
    }

    public void record(String bizType, Long bizId, String bizNo, String from, String to, String action, String reason) {
        FlowLog log = new FlowLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setBizNo(bizNo);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setAction(action);
        log.setReason(reason);
        log.setOperator(UserContext.name());
        log.setCreateTime(LocalDateTime.now());
        flowLogMapper.insert(log);
    }

    public void oper(String module, String action, String bizNo, String detail) {
        SysLog log = new SysLog();
        log.setUserId(UserContext.userId());
        log.setUserName(UserContext.name());
        log.setModule(module);
        log.setAction(action);
        log.setDetail((bizNo == null ? "" : "[" + bizNo + "] ") + detail);
        log.setIp("127.0.0.1");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 给指定岗位推送站内消息，消息带业务编号与主键，前端可直接跳转详情 */
    public void notify(String title, String bizType, Long bizId, String bizNo, String roleCode) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        for (SysUser u : users) {
            SysMessage m = new SysMessage();
            m.setTitle(title);
            m.setContent(title);
            m.setBizType(bizType);
            m.setBizNo(bizNo);
            m.setBizId(bizId);
            m.setUserId(u.getId());
            m.setIsRead(0);
            m.setCreateTime(LocalDateTime.now());
            messageMapper.insert(m);
            if (roleCode == null) {
                break;
            }
        }
    }

    public void notifyUser(Long userId, String title, String bizType, Long bizId, String bizNo) {
        SysMessage m = new SysMessage();
        m.setMsgType(bizType);
        m.setTitle(title);
        m.setContent(title);
        m.setBizType(bizType);
        m.setBizNo(bizNo);
        m.setBizId(bizId);
        m.setUserId(userId);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }
}
