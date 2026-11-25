package com.grad.svs.controller;

import com.grad.svs.common.PageResult;
import com.grad.svs.common.R;
import com.grad.svs.entity.SysDict;
import com.grad.svs.entity.SysLog;
import com.grad.svs.entity.SysOrg;
import com.grad.svs.entity.SysRole;
import com.grad.svs.service.SystemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理接口：用户、角色、菜单与分配菜单、日志、字典。
 * 分配菜单接口同样过滤「菜单管理」，防止越权下发。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/user/page")
    public R<PageResult<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long roleId) {
        return R.ok(systemService.users(page, size, keyword, orgId, roleId));
    }

    @PostMapping("/user")
    public R<Long> saveUser(@RequestBody Map<String, Object> body) {
        return R.ok(systemService.saveUser(body));
    }

    @PutMapping("/user/{id}")
    public R<Long> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(systemService.saveUser(body));
    }

    @PostMapping("/user/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }

    @GetMapping("/role/list")
    public R<List<SysRole>> roles() {
        return R.ok(systemService.roles());
    }

    @PostMapping("/role")
    public R<Long> saveRole(@RequestBody Map<String, Object> body) {
        return R.ok(systemService.saveRole(body));
    }

    @GetMapping("/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        return R.ok(systemService.menuTree(true));
    }

    @PostMapping("/menu")
    public R<Long> saveMenu(@RequestBody Map<String, Object> body) {
        return R.ok(systemService.saveMenu(body));
    }

    @GetMapping("/role/{roleId}/menus")
    public R<List<Long>> roleMenus(@PathVariable Long roleId) {
        return R.ok(systemService.roleMenus(roleId));
    }

    @PostMapping("/role/{roleId}/menus")
    public R<Void> assignMenus(@PathVariable Long roleId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("menuIds");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(x -> Long.valueOf(String.valueOf(x))).toList();
        systemService.assignMenus(roleId, ids);
        return R.ok();
    }

    @GetMapping("/log/page")
    public R<PageResult<SysLog>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(systemService.logs(page, size, keyword));
    }

    @GetMapping("/org/list")
    public R<List<SysOrg>> orgs() {
        return R.ok(systemService.orgs());
    }

    @GetMapping("/dict/{type}")
    public R<List<SysDict>> dicts(@PathVariable String type) {
        return R.ok(systemService.dicts(type));
    }
}
