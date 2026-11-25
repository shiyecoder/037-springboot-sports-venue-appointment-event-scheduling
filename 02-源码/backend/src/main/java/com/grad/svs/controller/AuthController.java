package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.service.AuthService;
import com.grad.svs.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 认证与个人中心接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FileService fileService;

    public AuthController(AuthService authService, FileService fileService) {
        this.authService = authService;
        this.fileService = fileService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(authService.login(body.get("username"), body.get("password")));
    }

    @GetMapping("/menus")
    public R<List<Map<String, Object>>> menus(@RequestParam Long roleId) {
        return R.ok(authService.menuTree(roleId));
    }

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        return R.ok(authService.profile());
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body) {
        authService.updateProfile(body.get("realName"), body.get("phone"), body.get("email"));
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    /** 头像上传后立即返回相对路径，前端同步刷新右上角头像 */
    @PostMapping("/avatar")
    public R<Map<String, Object>> avatar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> uploaded = fileService.upload(file, "avatar");
        String path = authService.updateAvatar(String.valueOf(uploaded.get("path")));
        uploaded.put("avatar", path);
        return R.ok(uploaded);
    }
}
