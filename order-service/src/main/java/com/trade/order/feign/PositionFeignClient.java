package com.trade.order.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 持仓服务 Feign 客户端
 */
@FeignClient(name = "trade-service", contextId = "positionFeignClient", path = "/position", fallback = PositionFeignClient.PositionFeignClientFallback.class)
public interface PositionFeignClient {

    /**
     * 买入建仓/加仓
     */
    @PostMapping("/buy")
    R<Void> buy(@RequestParam Long userId,
                @RequestParam Long productId,
                @RequestParam String productName,
                @RequestParam Integer quantity,
                @RequestParam BigDecimal price);

    /**
     * 卖出减仓
     */
    @PostMapping("/sell")
    R<Void> sell(@RequestParam Long userId,
                 @RequestParam Long productId,
                 @RequestParam Integer quantity,
                 @RequestParam BigDecimal price);

    /**
     * 熔断降级实现
     */
    class PositionFeignClientFallback implements PositionFeignClient {
        @Override
        public R<Void> buy(Long userId, Long productId, String productName, Integer quantity, BigDecimal price) {
            throw new RuntimeException("持仓服务不可用，请稍后重试");
        }

        @Override
        public R<Void> sell(Long userId, Long productId, Integer quantity, BigDecimal price) {
            throw new RuntimeException("持仓服务不可用，请稍后重试");
        }
    }
}
