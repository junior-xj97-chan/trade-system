package com.trade.quote.feign;

import com.trade.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("UserFeignClient fallback triggered: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public R<Boolean> checkUserExists(Long userId) {
                return R.fail("User service unavailable");
            }
        };
    }
}
