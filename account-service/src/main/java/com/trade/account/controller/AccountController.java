package com.trade.account.controller;

import com.trade.account.entity.Account;
import com.trade.account.service.AccountService;
import com.trade.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Tag(name = "账户管理")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create")
    @Operation(summary = "创建账户")
    public R<Long> create(@RequestBody CreateAccountRequest request) {
        return R.ok(accountService.createAccount(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "查询账户")
    public R<Account> getByUserId(@PathVariable Long userId) {
        return R.ok(accountService.getByUserId(userId));
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结资金")
    public R<Void> freeze(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount) {
        accountService.freezeAmount(userId, amount);
        return R.ok();
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻资金")
    public R<Void> unfreeze(@RequestParam("userId") Long userId,
                            @RequestParam("amount") BigDecimal amount) {
        accountService.unfreezeAmount(userId, amount);
        return R.ok();
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减余额")
    public R<Void> deduct(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount) {
        accountService.deductBalance(userId, amount);
        return R.ok();
    }

    @PostMapping("/recharge")
    @Operation(summary = "充值")
    public R<Void> recharge(@RequestParam("userId") Long userId,
                            @RequestParam("amount") BigDecimal amount) {
        accountService.recharge(userId, amount);
        return R.ok();
    }

    @PostMapping("/refund")
    @Operation(summary = "退款")
    public R<Void> refund(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount) {
        accountService.refund(userId, amount);
        return R.ok();
    }

    @PostMapping("/sellReceive")
    @Operation(summary = "卖出收款（卖出订单支付时增加余额）")
    public R<Void> sellReceive(@RequestParam("userId") Long userId,
                                @RequestParam("amount") BigDecimal amount) {
        accountService.sellReceive(userId, amount);
        return R.ok();
    }

    @lombok.Data
    public static class CreateAccountRequest {
        private Long userId;
        private BigDecimal initialBalance;
    }
}
