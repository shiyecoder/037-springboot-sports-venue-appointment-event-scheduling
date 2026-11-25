package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.svs.common.BizException;
import com.grad.svs.common.JwtUtil;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证与个人中心服务：登录失败锁定、菜单权限下发、资料与密码修改。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysOrgMapper orgMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Value("${app.login-fail-limit:5}")
    private int failLimit;
    @Value("${app.login-lock-minutes:10}")
    private int lockMinutes;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysOrgMapper orgMapper,
                       SysMenuMapper menuMapper, SysRoleMenuMapper roleMenuMapper,
                       PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.orgMapper = orgMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    /** 登录：校验锁定、比对密码、失败计数、成功后签发 JWT 并返回菜单 */
    @Transactional
    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BizException("请输入账号和密码");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim()));
        if (user == null) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用，请联系管理员");
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException("连续登录失败次数过多，账号已锁定至 " + user.getLockUntil().withNano(0));
        }
        if (!encoder.matches(password, user.getPassword())) {
            int fail = (user.getLoginFail() == null ? 0 : user.getLoginFail()) + 1;
            user.setLoginFail(fail);
            if (fail >= failLimit) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                user.setLoginFail(0);
                userMapper.updateById(user);
                throw new BizException("密码连续错误 " + failLimit + " 次，账号已锁定 " + lockMinutes + " 分钟");
            }
            userMapper.updateById(user);
            throw new BizException("账号或密码错误，还可尝试 " + (failLimit - fail) + " 次");
        }
        user.setLoginFail(0);
        user.setLockUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);

        SysRole role = roleMapper.selectById(user.getRoleId());
        SysOrg org = user.getOrgId() == null ? null : orgMapper.selectById(user.getOrgId());
        String token = jwtUtil.create(user.getId(), user.getRealName(),
                role == null ? "REQUESTER" : role.getRoleCode(), user.getOrgId(),
                role == null ? "SELF" : role.getDataScope());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", userVo(user, role, org));
        data.put("menus", menuTree(user.getRoleId()));
        return data;
    }

    public Map<String, Object> userVo(SysUser user, SysRole role, SysOrg org) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("realName", user.getRealName());
        vo.put("avatar", user.getAvatar());
        vo.put("phone", user.getPhone());
        vo.put("email", user.getEmail());
        vo.put("orgId", user.getOrgId());
        vo.put("orgName", org == null ? "" : org.getOrgName());
        vo.put("roleCode", role == null ? "" : role.getRoleCode());
        vo.put("roleName", role == null ? "" : role.getRoleName());
        vo.put("dataScope", role == null ? "SELF" : role.getDataScope());
        return vo;
    }

    /** 按角色返回菜单树；菜单管理仅对管理员下发 */
    public List<Map<String, Object>> menuTree(Long roleId) {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        Set<Long> allowed = new HashSet<>();
        if (roleId != null) {
            roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                    .forEach(rm -> allowed.add(rm.getMenuId()));
        }
        boolean admin = isAdminRole(roleId);
        List<SysMenu> visible = all.stream()
                .filter(m -> allowed.contains(m.getId()))
                .filter(m -> !"B".equals(m.getMenuType()))
                .filter(m -> admin || !"system:menu".equals(m.getPerm()))
                .filter(m -> m.getVisible() == null || m.getVisible() == 1)
                .collect(Collectors.toList());
        return buildTree(visible, 0L);
    }

    private boolean isAdminRole(Long roleId) {
        SysRole r = roleId == null ? null : roleMapper.selectById(roleId);
        return r != null && "ADMIN".equals(r.getRoleCode());
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> list, Long parent) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysMenu m : list) {
            if (Objects.equals(m.getParentId() == null ? 0L : m.getParentId(), parent)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", m.getId());
                node.put("name", m.getMenuName());
                node.put("path", m.getPath());
                node.put("icon", m.getIcon());
                node.put("perm", m.getPerm());
                node.put("type", m.getMenuType());
                List<Map<String, Object>> children = buildTree(list, m.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                out.add(node);
            }
        }
        return out;
    }

    public Map<String, Object> profile() {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw new BizException(401, "登录已过期");
        }
        return userVo(user, roleMapper.selectById(user.getRoleId()),
                user.getOrgId() == null ? null : orgMapper.selectById(user.getOrgId()));
    }

    @Transactional
    public void updateProfile(String realName, String phone, String email) {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (realName != null && !realName.isBlank()) {
            user.setRealName(realName.trim());
        }
        user.setPhone(phone);
        user.setEmail(email);
        userMapper.updateById(user);
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Transactional
    public String updateAvatar(String path) {
        SysUser user = userMapper.selectById(UserContext.userId());
        user.setAvatar(path);
        userMapper.updateById(user);
        return path;
    }
}
