package com.trade.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.trade.account.controller.AccountController.CreateAccountRequest;
import com.trade.account.entity.Account;
import com.trade.account.mapper.AccountMapper;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 幂等 Key 前缀
    private static final String IDEMPOTENT_PREFIX = "account:deduct:";
    // 幂等过期时间（分钟）
    private static final long IDEMPOTENT_EXPIRE_MINUTES = 30;

    /**
     * 创建账户
     */
    public Long createAccount(CreateAccountRequest request) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUserId, request.getUserId());
        if (accountMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND.getCode(), "账户已存在");
        }

        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setStatus(1);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        
        accountMapper.insert(account);
        return account.getId();
    }

    /**
     * 冻结资金（支付时调用）
     *
     * @param orderId 订单ID，作为幂等键业务流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void freezeAmount(Long userId, BigDecimal amount, Long orderId) {
        // ========== 幂等性检查：基于业务流水号防止重复冻结 ==========
        String idempotentKey = orderId != null
                ? "account:freeze:" + orderId
                : "account:freeze:" + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            // 已处理过，幂等返回（不再抛异常，避免补偿重试失败）
            log.info("【冻结资金-幂等命中】userId={}，orderId={}", userId, orderId);
            return;
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == 0) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }

        BigDecimal available = account.getBalance().subtract(account.getFrozenAmount());
        if (available.compareTo(amount) < 0) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH);
        }

        account.setFrozenAmount(account.getFrozenAmount().add(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getFrozenAmount, account.getFrozenAmount())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    /**
     * 解冻资金（退款时调用）
     *
     * @param orderId 订单ID，作为幂等键业务流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAmount(Long userId, BigDecimal amount, Long orderId) {
        // ========== 幂等性检查：基于业务流水号防止重复解冻 ==========
        String idempotentKey = orderId != null
                ? "account:unfreeze:" + orderId
                : "account:unfreeze:" + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.info("【解冻资金-幂等命中】userId={}，orderId={}", userId, orderId);
            return;
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getFrozenAmount().compareTo(amount) < 0) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH.getCode(), "冻结金额不足");
        }

        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getFrozenAmount, account.getFrozenAmount())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    /**
     * 扣减余额（实际支付时调用）
     * 从冻结金额中扣减，并减少余额
     *
     * @param orderId 订单ID，作为幂等键业务流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, BigDecimal amount, Long orderId) {
        // ========== 幂等性检查：基于业务流水号防止重复扣减 ==========
        String idempotentKey = orderId != null
                ? IDEMPOTENT_PREFIX + "deduct:" + orderId
                : IDEMPOTENT_PREFIX + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.info("【扣减余额-幂等命中】userId={}，orderId={}", userId, orderId);
            return;
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }

        BigDecimal frozen = account.getFrozenAmount();
        if (frozen.compareTo(amount) < 0) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH.getCode(), "冻结金额不足");
        }

        // 扣减冻结金额，同时减少余额
        account.setFrozenAmount(frozen.subtract(amount));
        account.setBalance(account.getBalance().subtract(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getFrozenAmount, account.getFrozenAmount())
               .set(Account::getBalance, account.getBalance())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    /**
     * 充值
     */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        // ========== 幂等性检查：防止重复充值 ==========
        String idempotentKey = "account:recharge:" + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }

        account.setBalance(account.getBalance().add(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getBalance, account.getBalance())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    /**
     * 退款（取消订单时调用）
     *
     * @param orderId 订单ID，作为幂等键业务流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, BigDecimal amount, Long orderId) {
        // ========== 幂等性检查：基于业务流水号防止重复退款 ==========
        String idempotentKey = orderId != null
                ? "account:refund:" + orderId
                : "account:refund:" + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.info("【退款-幂等命中】userId={}，orderId={}", userId, orderId);
            return;
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }

        // 退款增加余额
        account.setBalance(account.getBalance().add(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getBalance, account.getBalance())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    /**
     * 卖出收款（卖出订单支付时调用）
     * 卖出股票获得资金：增加用户余额
     *
     * @param orderId 订单ID，作为幂等键业务流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void sellReceive(Long userId, BigDecimal amount, Long orderId) {
        // ========== 幂等性检查：基于业务流水号防止重复收款 ==========
        String idempotentKey = orderId != null
                ? "account:sellReceive:" + orderId
                : "account:sellReceive:" + userId + ":" + amount.toPlainString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.info("【卖出收款-幂等命中】userId={}，orderId={}", userId, orderId);
            return;
        }

        Account account = getByUserId(userId);
        if (account == null) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == 0) {
            redisTemplate.delete(idempotentKey);
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }

        account.setBalance(account.getBalance().add(amount));
        // 使用 UpdateWrapper 避免乐观锁参数问题
        LambdaUpdateWrapper<Account> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Account::getId, account.getId())
               .set(Account::getBalance, account.getBalance())
               .set(Account::getUpdateTime, LocalDateTime.now());
        accountMapper.update(null, wrapper);
    }

    public Account getByUserId(Long userId) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUserId, userId);
        Account account = accountMapper.selectOne(wrapper);
        
        // 兜底逻辑：如果账户不存在，自动创建一个（初始余额为0）
        if (account == null) {
            account = new Account();
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO);
            account.setFrozenAmount(BigDecimal.ZERO);
            account.setStatus(1);
            account.setCreateTime(LocalDateTime.now());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.insert(account);
        }
        
        return account;
    }
}
