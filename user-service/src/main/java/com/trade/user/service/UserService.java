package com.trade.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.PageResult;
import com.trade.user.controller.UserController.*;
import com.trade.user.entity.User;
import com.trade.user.mapper.UserMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "user:token:";

    /**
     * 用户注册
     */
    public Long register(RegisterRequest request) {
        // 校验用户名唯一
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BizCode.USER_EXIST);
        }

        // 保存用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 用户登录
     */
    public LoginResp login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }
        if (!user.getPassword().equals(md5(request.getPassword()))) {
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }

        // 生成 token
        String token = UUID.randomUUID().toString().replace("-", "");
        // 用 String 序列化 userId，避免 Gateway（Jackson）反序列化 JDK 二进制值失败
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, String.valueOf(user.getId()), 2, TimeUnit.HOURS);
        // 返回完整的登录响应（包含 token、userId、username），与前端 LoginResp 对齐
        return new LoginResp("Bearer " + token, user.getId(), user.getUsername());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResp {
        private String token;
        private Long userId;
        private String username;
    }

    /**
     * 分页查询
     */
    public PageResult<User> page(Long current, Long size) {
        Page<User> page = new Page<>(current, size);
        Page<User> result = userMapper.selectPage(page, null);
        return PageResult.of(result.getRecords(), result.getTotal(), current, size);
    }

    /**
     * 修改状态
     */
    public void updateStatus(Long id, Integer status) {
        // ========== 幂等性检查：防止重复修改状态 ==========
        String idempotentKey = "user:status:" + id;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", 30, TimeUnit.MINUTES);
        if (!success) {
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 根据ID查询用户
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    private String md5(String str) {
        return DigestUtils.md5DigestAsHex(("trade:" + str).getBytes());
    }
}
