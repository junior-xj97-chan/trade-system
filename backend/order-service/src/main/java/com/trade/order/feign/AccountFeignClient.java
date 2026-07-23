package com.trade.order.feign;

import com.trade.common.R;
import com.trade.order.feign.fallback.AccountFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 账户服务 Feign 客户端
 * fallbackFactory：当账户服务异常/熔断时，返回降级结果
 */
@FeignClient(name = "account-service", path = "/account", fallbackFactory = AccountFeignFallbackFactory.class)
public interface AccountFeignClient {

    /**
     * 冻结用户资金（下单时调用）
     */
    @PostMapping("/freeze")
    R<Void> freezeAmount(@RequestParam("userId") Long userId,
                         @RequestParam("amount") BigDecimal amount,
                         @RequestParam("orderId") Long orderId);

    /**
     * 扣减余额（支付时调用）
     */
    @PostMapping("/deduct")
    R<Void> deductBalance(@RequestParam("userId") Long userId,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam("orderId") Long orderId);

    /**
     * 退款（取消订单时调用）
     */
    @PostMapping("/refund")
    R<Void> refund(@RequestParam("userId") Long userId,
                   @RequestParam("amount") BigDecimal amount,
                   @RequestParam("orderId") Long orderId);

    /**
     * 卖出收款（卖出订单支付时调用，增加用户余额）
     */
    @PostMapping("/sellReceive")
    R<Void> sellReceive(@RequestParam("userId") Long userId,
                        @RequestParam("amount") BigDecimal amount,
                        @RequestParam("orderId") Long orderId);
}
