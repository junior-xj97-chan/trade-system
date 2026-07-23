package com.trade.user.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "account-service", fallback = AccountFeignFallback.class)
public interface AccountFeignClient {

    @PostMapping("/account/create")
    R<Long> createAccount(@RequestBody CreateAccountRequest request);

    record CreateAccountRequest(Long userId, BigDecimal initialBalance) {}
}