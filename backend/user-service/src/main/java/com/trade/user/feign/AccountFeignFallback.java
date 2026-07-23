package com.trade.user.feign;

import com.trade.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class AccountFeignFallback implements AccountFeignClient {

    @Override
    public R<Long> createAccount(CreateAccountRequest request) {
        log.error("创建账户失败，account-service 不可用，userId={}", request.userId());
        return R.fail("创建账户失败");
    }
}