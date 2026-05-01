package com.trade.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.user.entity.User;
import com.trade.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public R<Long> register(@RequestBody RegisterRequest request) {
        return R.ok(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<UserService.LoginResp> login(@RequestBody LoginRequest request) {
        return R.ok(userService.login(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息")
    public R<User> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @GetMapping
    @Operation(summary = "分页查询用户")
    public R<PageResult<User>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return R.ok(userService.page(current, size));
    }

    @PutMapping("/status/{id}")
    @Operation(summary = "修改用户状态")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @PutMapping("/profile")
    @Operation(summary = "修改个人资料")
    public R<Void> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody UserService.UpdateProfileRequest request) {
        if (userId == null) throw new BusinessException(BizCode.USER_NOT_FOUND);
        userService.updateProfile(userId, request);
        return R.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody UserService.ChangePasswordRequest request) {
        if (userId == null) throw new BusinessException(BizCode.USER_NOT_FOUND);
        userService.changePassword(userId, request);
        return R.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.logout(token);
        return R.ok();
    }

    @PostMapping("/avatar")
    @Operation(summary = "上传头像")
    public R<String> uploadAvatar(@RequestHeader(value = "Authorization", required = false) String token,
                                  @RequestParam("file") MultipartFile file) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(BizCode.TOKEN_INVALID);
        }
        String url = userService.uploadAvatar(token.substring(7), file);
        return R.ok(url);
    }

    // ---- Request DTO ----

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String phone;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
