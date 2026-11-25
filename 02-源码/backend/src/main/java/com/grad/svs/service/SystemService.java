package com.grad.svs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.svs.common.BizException;
import com.grad.svs.common.PageResult;
import com.grad.svs.common.UserContext;
import com.grad.svs.entity.*;
import com.grad.svs.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理：用户、角色、菜单、角色菜单分配、操作日志、字典。
 * 菜单权限、按钮权限、数据范围分别控制；「菜单管理」仅对管理员可见。
 */
@Service
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysOrgMapper orgMapper;
    private final SysLogMapper logMapper;
    private final SysDictMapper dictMapper;
    private final PasswordEncoder encoder;

    public SystemService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                         SysRoleMenuMapper roleMenuMapper, SysOrgMapper orgMapper, SysLogMapper logMapper,
                         SysDictMapper dictMapper, PasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.orgMapper = orgMapper;
        this.logMapper = logMapper;
        this.dictMapper = dictMapper;
        this.encoder = encoder;
    }

    public PageResult<Map<String, Object>> users(int page, int size, String keyword, Long orgId, Long roleId) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword));
        }
        if (orgId != null) {
            w.eq(SysUser::getOrgId, orgId);
        }
        if (roleId != null) {
            w.eq(SysUser::getRoleId, roleId);
        }
        w.orderByAsc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> orgNames = new HashMap<>();
        orgMapper.selectList(null).forEach(o -> orgNames.put(o.getId(), o.getOrgName()));
        Map<Long, String> roleNames = new HashMap<>();
        roleMapper.selectList(null).forEach(r -> roleNames.put(r.getId(), r.getRoleName()));
        List<Map<String, Object>> list = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("avatar", u.getAvatar());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("orgId", u.getOrgId());
            m.put("orgName", orgNames.getOrDefault(u.getOrgId(), "-"));
            m.put("roleId", u.getRoleId());
            m.put("roleName", roleNames.getOrDefault(u.getRoleId(), "-"));
            m.put("status", u.getStatus());
            m.put("lastLogin", u.getLastLogin());
            m.put("createTime", u.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), list);
    }

    @Transactional
    public Long saveUser(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysUser u = id == null ? new SysUser() : userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        String username = str(body.get("username"));
        String realName = str(body.get("realName"));
        if (realName == null || realName.isBlank()) {
            throw new BizException("姓名不能为空");
        }
        u.setRealName(realName.trim());
        u.setPhone(str(body.get("phone")));
        u.setEmail(str(body.get("email")));
        u.setOrgId(body.get("orgId") == null ? null : Long.valueOf(String.valueOf(body.get("orgId"))));
        u.setRoleId(body.get("roleId") == null ? null : Long.valueOf(String.valueOf(body.get("roleId"))));
        u.setStatus(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id == null) {
            if (username == null || username.isBlank()) {
                throw new BizException("登录账号不能为空");
            }
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
                throw new BizException("登录账号已存在");
            }
            u.setUsername(username.trim());
            u.setPassword(encoder.encode(body.get("password") == null ? "123456" : String.valueOf(body.get("password"))));
            u.setLoginFail(0);
            u.setCreateTime(LocalDateTime.now());
            u.setUpdateTime(LocalDateTime.now());
            userMapper.insert(u);
        } else {
            userMapper.updateById(u);
        }
        return u.getId();
    }

    @Transactional
    public void resetPassword(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        u.setPassword(encoder.encode("123456"));
        u.setLoginFail(0);
        u.setLockUntil(null);
        userMapper.updateById(u);
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Transactional
    public Long saveRole(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysRole r = id == null ? new SysRole() : roleMapper.selectById(id);
        if (r == null) {
            throw new BizException("角色不存在");
        }
        String name = str(body.get("roleName"));
        if (name == null || name.isBlank()) {
            throw new BizException("角色名称不能为空");
        }
        r.setRoleName(name.trim());
        r.setRoleCode(body.get("roleCode") == null ? r.getRoleCode() : str(body.get("roleCode")));
        r.setDataScope(body.get("dataScope") == null ? "SELF" : str(body.get("dataScope")));
        r.setRemark(str(body.get("remark")));
        r.setStatus(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id == null) {
            r.setCreateTime(LocalDateTime.now());
            roleMapper.insert(r);
        } else {
            roleMapper.updateById(r);
        }
        return r.getId();
    }

    /** 菜单树（含按钮），管理员可见全部；其他角色过滤掉「菜单管理」 */
    public List<Map<String, Object>> menuTree(boolean forAssign) {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        boolean admin = UserContext.isAdmin();
        List<SysMenu> filtered = all.stream()
                .filter(m -> admin || m.getId() != 24L)
                .collect(Collectors.toList());
        return build(filtered, 0L, forAssign);
    }

    private List<Map<String, Object>> build(List<SysMenu> list, Long parent, boolean forAssign) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysMenu m : list) {
            if (Objects.equals(m.getParentId() == null ? 0L : m.getParentId(), parent)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", m.getId());
                node.put("name", m.getMenuName());
                node.put("type", m.getMenuType());
                node.put("path", m.getPath());
                node.put("perm", m.getPerm());
                node.put("sort", m.getSort());
                node.put("visible", m.getVisible());
                List<Map<String, Object>> children = build(list, m.getId(), forAssign);
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                out.add(node);
            }
        }
        return out;
    }

    public List<Long> roleMenus(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        if (menuIds.contains(24L) && !"ADMIN".equals(role.getRoleCode())) {
            throw new BizException("「菜单管理」只允许分配给系统管理员");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long mid : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            roleMenuMapper.insert(rm);
        }
    }

    @Transactional
    public Long saveMenu(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysMenu m = id == null ? new SysMenu() : menuMapper.selectById(id);
        if (m == null) {
            throw new BizException("菜单不存在");
        }
        String name = str(body.get("menuName"));
        if (name == null || name.isBlank()) {
            throw new BizException("菜单名称不能为空");
        }
        m.setMenuName(name.trim());
        m.setParentId(body.get("parentId") == null ? 0L : Long.valueOf(String.valueOf(body.get("parentId"))));
        m.setMenuType(body.get("menuType") == null ? "C" : str(body.get("menuType")));
        m.setPath(str(body.get("path")));
        m.setPerm(str(body.get("perm")));
        m.setIcon(str(body.get("icon")));
        m.setSort(body.get("sort") == null ? 0 : Integer.valueOf(String.valueOf(body.get("sort"))));
        m.setVisible(body.get("visible") == null ? 1 : Integer.valueOf(String.valueOf(body.get("visible"))));
        if (id == null) {
            m.setCreateTime(LocalDateTime.now());
            menuMapper.insert(m);
        } else {
            menuMapper.updateById(m);
        }
        return m.getId();
    }

    public PageResult<SysLog> logs(int page, int size, String keyword) {
        LambdaQueryWrapper<SysLog> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(SysLog::getUserName, keyword).or().like(SysLog::getModule, keyword)
                    .or().like(SysLog::getDetail, keyword));
        }
        w.orderByDesc(SysLog::getId);
        Page<SysLog> p = logMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    public List<SysOrg> orgs() {
        return orgMapper.selectList(new LambdaQueryWrapper<SysOrg>().orderByAsc(SysOrg::getSort));
    }

    public List<SysDict> dicts(String type) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(type != null && !type.isBlank(), SysDict::getDictType, type)
                .orderByAsc(SysDict::getSort));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
