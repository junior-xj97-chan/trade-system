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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * 修改个人资料（昵称、邮箱、手机、性别、头像）
     */
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }
        User u = new User();
        u.setId(userId);
        if (request.getNickname() != null) u.setNickname(request.getNickname());
        if (request.getEmail() != null) u.setEmail(request.getEmail());
        if (request.getPhone() != null) u.setPhone(request.getPhone());
        if (request.getGender() != null) u.setGender(request.getGender());
        if (request.getAvatar() != null) u.setAvatar(request.getAvatar());
        userMapper.updateById(u);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }
        // 校验旧密码
        if (!user.getPassword().equals(md5(request.getOldPassword()))) {
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }
        // 更新新密码
        User u = new User();
        u.setId(userId);
        u.setPassword(md5(request.getNewPassword()));
        userMapper.updateById(u);
    }

    /**
     * 退出登录（服务端使 Token 失效）
     */
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        redisTemplate.delete(TOKEN_PREFIX + token);
    }

    /**
     * 上传头像
     * 保存到本地 uploads/avatars/ 目录，返回 /static/avatars/ 访问路径
     */
    public String uploadAvatar(String token, MultipartFile file) {
        // 从 Redis 根据 token 查询 userId
        String userIdStr = (String) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userIdStr == null) {
            throw new BusinessException(BizCode.TOKEN_INVALID);
        }
        Long userId = Long.parseLong(userIdStr);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        // 校验文件类型（仅允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }
        // 不允许的文件类型黑名单
        if (contentType.equals("image/svg+xml") || contentType.equals("image/x-icon")) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }

        // 生成唯一文件名：userId_时间戳_随机UUID.扩展名
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        // 保存到本地 uploads/avatars/ 目录
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "avatars";
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 构造访问 URL（前端通过 /static 代理访问，不经过 Gateway）
            String avatarUrl = "/static/avatars/" + filename;

            // 同时更新数据库中的 avatar 字段
            User u = new User();
            u.setId(userId);
            u.setAvatar(avatarUrl);
            userMapper.updateById(u);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("头像保存失败: " + e.getMessage(), e);
        }
    }

    // ---- Request DTO ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String nickname;
        private String email;
        private String phone;
        private Integer gender;
        private String avatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    private String md5(String str) {
        return DigestUtils.md5DigestAsHex(("trade:" + str).getBytes());
    }
}
