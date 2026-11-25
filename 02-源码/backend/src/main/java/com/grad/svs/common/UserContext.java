package com.grad.svs.common;

/**
 * 当前登录人上下文（ThreadLocal）。
 * 数据范围：SELF 本人 / DEPT 本部门 / ORG 本组织 / ALL 全部 / ORG_TREE 授权组织范围。
 */
public class UserContext {

    public record LoginUser(Long id, String name, String roleCode, Long orgId, String dataScope) {
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.id();
    }

    public static String name() {
        LoginUser u = HOLDER.get();
        return u == null ? "系统" : u.name();
    }

    public static String role() {
        LoginUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static Long orgId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
