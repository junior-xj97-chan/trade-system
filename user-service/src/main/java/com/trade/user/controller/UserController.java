package com.trade.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.user.entity.User;
import com.trade.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public R<String> login(@RequestBody LoginRequest request) {
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
