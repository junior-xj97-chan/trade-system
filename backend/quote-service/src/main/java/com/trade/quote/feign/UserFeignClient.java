package com.trade.quote.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "quoteUserFeignClient")
public interface UserFeignClient {

    @GetMapping("/user/check/{userId}")
    R<Boolean> checkUserExists(@PathVariable("userId") Long userId);
}
