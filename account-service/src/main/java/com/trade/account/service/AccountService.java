package com.trade.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.account.controller.AccountController.CreateAccountRequest;
import com.trade.account.entity.Account;
import com.trade.account.mapper.AccountMapper;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;

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
     * Seata 会保证分布式事务一致性
     */
    @Transactional(rollbackFor = Exception.class)
    public void freezeAmount(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == 0) {
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }
        
        BigDecimal available = account.getBalance().subtract(account.getFrozenAmount());
        if (available.compareTo(amount) < 0) {
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH);
        }

        account.setFrozenAmount(account.getFrozenAmount().add(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /**
     * 解冻资金（退款时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAmount(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getFrozenAmount().compareTo(amount) < 0) {
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH.getCode(), "冻结金额不足");
        }

        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /**
     * 扣减余额（实际支付时调用）
     * 从冻结金额中扣减，并减少余额
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        
        BigDecimal frozen = account.getFrozenAmount();
        if (frozen.compareTo(amount) < 0) {
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH.getCode(), "冻结金额不足");
        }

        // 扣减冻结金额，同时减少余额
        account.setFrozenAmount(frozen.subtract(amount));
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /**
     * 充值
     */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }

        account.setBalance(account.getBalance().add(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /**
     * 退款（取消订单时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }

        // 退款增加余额
        account.setBalance(account.getBalance().add(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /**
     * 卖出收款（卖出订单支付时调用）
     * 卖出股票获得资金：增加用户余额
     */
    @Transactional(rollbackFor = Exception.class)
    public void sellReceive(Long userId, BigDecimal amount) {
        Account account = getByUserId(userId);
        if (account == null) {
            throw new BusinessException(BizCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == 0) {
            throw new BusinessException(BizCode.ACCOUNT_FROZEN);
        }

        account.setBalance(account.getBalance().add(amount));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);
    }

    public Account getByUserId(Long userId) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUserId, userId);
        return accountMapper.selectOne(wrapper);
    }
}
