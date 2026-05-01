package com.trade.order.feign.fallback;

import com.trade.common.R;
import com.trade.order.feign.AccountFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * AccountFeignClient 熔断降级处理器
 * 当账户服务不可用时，返回降级结果并记录日志
 */
@Slf4j
@Component
public class AccountFeignFallbackFactory implements FallbackFactory<AccountFeignClient> {

    @Override
    public AccountFeignClient create(Throwable cause) {
        // 打印原因，便于排查
        log.error("[AccountFeignClient] 服务熔断降级，原因: {}", cause.getMessage());

        return new AccountFeignClient() {

            @Override
            public R<Void> freezeAmount(Long userId, BigDecimal amount) {
                log.warn("[AccountFeignClient#freezeAmount] 熔断降级 userId={}, amount={}", userId, amount);
                return R.fail("账户服务暂不可用，冻结资金失败，请稍后重试");
            }

            @Override
            public R<Void> deductBalance(Long userId, BigDecimal amount) {
                log.warn("[AccountFeignClient#deductBalance] 熔断降级 userId={}, amount={}", userId, amount);
                return R.fail("账户服务暂不可用，扣减余额失败，请稍后重试");
            }

            @Override
            public R<Void> refund(Long userId, BigDecimal amount) {
                log.warn("[AccountFeignClient#refund] 熔断降级 userId={}, amount={}", userId, amount);
                return R.fail("账户服务暂不可用，退款失败，请稍后重试");
            }

            @Override
            public R<Void> sellReceive(Long userId, BigDecimal amount) {
                log.warn("[AccountFeignClient#sellReceive] 熔断降级 userId={}, amount={}", userId, amount);
                return R.fail("账户服务暂不可用，卖出收款失败，请稍后重试");
            }
        };
    }
}
