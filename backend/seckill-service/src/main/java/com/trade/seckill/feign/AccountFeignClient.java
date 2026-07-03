package com.trade.seckill.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "account-service", path = "/account")
public interface AccountFeignClient {

    @PostMapping("/deduct")
    R<Void> deductBalance(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount);
}
