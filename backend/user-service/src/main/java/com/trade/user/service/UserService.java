package com.trade.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.user.controller.UserController.*;
import com.trade.user.entity.User;
import com.trade.user.feign.AccountFeignClient;
import com.trade.user.mapper.UserMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AccountFeignClient accountFeignClient;

    private static final String TOKEN_PREFIX = "user:token:";
    private static final String REFRESH_TOKEN_PREFIX = "user:refresh:";
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private static final long ACCESS_TOKEN_EXPIRE_MINUTES = 30;
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7;

    private static final String LOGIN_FAIL_PREFIX = "user:login_fail:";
    private static final String ACCOUNT_LOCK_PREFIX = "user:account_lock:";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 15;

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
        user.setPassword(encryptPassword(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        // 注册成功后自动创建账户（初始余额为0）
        try {
            R<Long> result = accountFeignClient.createAccount(new AccountFeignClient.CreateAccountRequest(user.getId(), BigDecimal.ZERO));
            if (result == null || !result.isSuccess()) {
                String msg = result != null ? result.getMessage() : "创建账户失败";
                if (!msg.contains("账户已存在")) {
                    throw new BusinessException(BizCode.SYSTEM_ERROR.getCode(), "用户注册成功，但账户创建失败，请联系客服");
                }
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("账户已存在")) {
                throw new BusinessException(BizCode.SYSTEM_ERROR.getCode(), "用户注册成功，但账户创建失败，请联系客服");
            }
        }

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

        String lockKey = ACCOUNT_LOCK_PREFIX + user.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BusinessException(BizCode.ACCOUNT_LOCKED);
        }

        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            incrementLoginFailCount(user.getId());
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }

        resetLoginFailCount(user.getId());

        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(TOKEN_PREFIX + accessToken, String.valueOf(user.getId()), ACCESS_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshToken, String.valueOf(user.getId()), REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

        return new LoginResp("Bearer " + accessToken, refreshToken, user.getId(), user.getUsername());
    }

    private void incrementLoginFailCount(Long userId) {
        String failKey = LOGIN_FAIL_PREFIX + userId;
        Long failCount = redisTemplate.opsForValue().increment(failKey);
        if (failCount == null) {
            redisTemplate.opsForValue().set(failKey, "1", LOCK_MINUTES, TimeUnit.MINUTES);
            failCount = 1L;
        }
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            String lockKey = ACCOUNT_LOCK_PREFIX + userId;
            redisTemplate.opsForValue().set(lockKey, "locked", LOCK_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
        }
    }

    private void resetLoginFailCount(Long userId) {
        redisTemplate.delete(LOGIN_FAIL_PREFIX + userId);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResp {
        private String token;
        private String refreshToken;
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
        if (!verifyPassword(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }
        // 更新新密码
        User u = new User();
        u.setId(userId);
        u.setPassword(encryptPassword(request.getNewPassword()));
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
     * 使用 Refresh Token 刷新 Access Token
     */
    public LoginResp refreshToken(String refreshToken) {
        String userIdStr = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + refreshToken);
        if (userIdStr == null) {
            throw new BusinessException(BizCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = Long.parseLong(userIdStr);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        String newAccessToken = UUID.randomUUID().toString().replace("-", "");
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        redisTemplate.opsForValue().set(TOKEN_PREFIX + newAccessToken, String.valueOf(userId), ACCESS_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + newRefreshToken, String.valueOf(userId), REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

        return new LoginResp("Bearer " + newAccessToken, newRefreshToken, userId, user.getUsername());
    }

    /**
     * 上传头像
     * 保存到本地 uploads/avatars/ 目录，返回 /static/avatars/ 访问路径
     */
    public String uploadAvatar(String token, MultipartFile file) {
        String userIdStr = (String) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userIdStr == null) {
            throw new BusinessException(BizCode.TOKEN_INVALID);
        }
        Long userId = Long.parseLong(userIdStr);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!isAllowedExtension(ext)) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(BizCode.PARAM_ERROR);
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new BusinessException(BizCode.PARAM_ERROR);
            }
            if (!validateImageMagicNumber(bytes, ext)) {
                throw new BusinessException(BizCode.PARAM_ERROR);
            }
        } catch (IOException e) {
            throw new BusinessException(BizCode.SYSTEM_ERROR.getCode(), "文件读取失败");
        }

        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "avatars";
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String avatarUrl = "/static/avatars/" + filename;

            User u = new User();
            u.setId(userId);
            u.setAvatar(avatarUrl);
            userMapper.updateById(u);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("头像保存失败: " + e.getMessage(), e);
        }
    }

    private boolean isAllowedExtension(String ext) {
        return List.of(".jpg", ".jpeg", ".png", ".gif").contains(ext);
    }

    private boolean validateImageMagicNumber(byte[] bytes, String ext) {
        if (bytes.length < 4) return false;

        int firstByte = bytes[0] & 0xFF;
        int secondByte = bytes[1] & 0xFF;
        int thirdByte = bytes[2] & 0xFF;
        int fourthByte = bytes[3] & 0xFF;

        switch (ext) {
            case ".jpg":
            case ".jpeg":
                return firstByte == 0xFF && secondByte == 0xD8;
            case ".png":
                return firstByte == 0x89 && secondByte == 0x50 && thirdByte == 0x4E && fourthByte == 0x47;
            case ".gif":
                return firstByte == 0x47 && secondByte == 0x49 && thirdByte == 0x46;
            default:
                return false;
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

    private String encryptPassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
