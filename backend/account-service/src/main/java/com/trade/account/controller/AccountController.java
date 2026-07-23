package com.trade.account.controller;

import com.trade.account.entity.Account;
import com.trade.account.service.AccountService;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.R;
import com.trade.common.config.FeignInternalInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Tag(name = "账户管理")
public class AccountController {

    private final AccountService accountService;
    private final HttpServletRequest request;

    @PostMapping("/create")
    @Operation(summary = "创建账户")
    public R<Long> create(@RequestBody CreateAccountRequest request) {
        assertInternalService();
        return R.ok(accountService.createAccount(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "查询账户")
    public R<Account> getByUserId(@PathVariable Long userId,
                                  @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (!isInternalService()) {
            if (currentUserId == null) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        }
        return R.ok(accountService.getByUserId(userId));
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结资金")
    public R<Void> freeze(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam(value = "orderId", required = false) Long orderId) {
        assertInternalService();
        accountService.freezeAmount(userId, amount, orderId);
        return R.ok();
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻资金")
    public R<Void> unfreeze(@RequestParam("userId") Long userId,
                            @RequestParam("amount") BigDecimal amount,
                            @RequestParam(value = "orderId", required = false) Long orderId) {
        assertInternalService();
        accountService.unfreezeAmount(userId, amount, orderId);
        return R.ok();
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减余额")
    public R<Void> deduct(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam(value = "orderId", required = false) Long orderId) {
        assertInternalService();
        accountService.deductBalance(userId, amount, orderId);
        return R.ok();
    }

    @PostMapping("/recharge")
    @Operation(summary = "充值")
    public R<Void> recharge(@RequestParam("userId") Long userId,
                            @RequestParam("amount") BigDecimal amount,
                            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessException(BizCode.PARAM_ERROR.getCode(), "充值金额不能低于 ¥1");
        }
        if (!isInternalService()) {
            if (currentUserId == null) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        }
        accountService.recharge(userId, amount);
        return R.ok();
    }

    @PostMapping("/refund")
    @Operation(summary = "退款")
    public R<Void> refund(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam(value = "orderId", required = false) Long orderId) {
        assertInternalService();
        accountService.refund(userId, amount, orderId);
        return R.ok();
    }

    @PostMapping("/sellReceive")
    @Operation(summary = "卖出收款（卖出订单支付时增加余额）")
    public R<Void> sellReceive(@RequestParam("userId") Long userId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "orderId", required = false) Long orderId) {
        assertInternalService();
        accountService.sellReceive(userId, amount, orderId);
        return R.ok();
    }

    private boolean isInternalService() {
        String header = request.getHeader(FeignInternalInterceptor.INTERNAL_HEADER);
        return FeignInternalInterceptor.INTERNAL_HEADER_VALUE.equals(header);
    }

    private void assertInternalService() {
        if (!isInternalService()) {
            throw new BusinessException(BizCode.INTERNAL_ACCESS_DENIED);
        }
    }

    @lombok.Data
    public static class CreateAccountRequest {
        private Long userId;
        private BigDecimal initialBalance;
    }
}
