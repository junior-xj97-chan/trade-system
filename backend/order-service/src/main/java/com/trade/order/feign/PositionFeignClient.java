package com.trade.order.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 持仓服务 Feign 客户端
 */
@FeignClient(name = "trade-service", contextId = "positionFeignClient", path = "/trade/position", fallback = PositionFeignClient.PositionFeignClientFallback.class)
public interface PositionFeignClient {

    /**
     * 买入建仓/加仓
     *
     * @param orderId 订单ID（用于幂等性控制，防止重复建仓/加仓）
     */
    @PostMapping("/buy")
    R<Void> buy(@RequestParam Long orderId,
                @RequestParam Long userId,
                @RequestParam Long productId,
                @RequestParam String productName,
                @RequestParam String productCode,
                @RequestParam Integer quantity,
                @RequestParam BigDecimal price);

    /**
     * 卖出减仓
     *
     * @param orderId 订单ID（用于幂等性控制，防止重复减仓）
     */
    @PostMapping("/sell")
    R<Void> sell(@RequestParam Long orderId,
                 @RequestParam Long userId,
                 @RequestParam Long productId,
                 @RequestParam Integer quantity,
                 @RequestParam BigDecimal price);

    /**
     * 熔断降级实现
     */
    class PositionFeignClientFallback implements PositionFeignClient {
        @Override
        public R<Void> buy(Long orderId, Long userId, Long productId, String productName, String productCode, Integer quantity, BigDecimal price) {
            throw new RuntimeException("持仓服务不可用，请稍后重试");
        }

        @Override
        public R<Void> sell(Long orderId, Long userId, Long productId, Integer quantity, BigDecimal price) {
            throw new RuntimeException("持仓服务不可用，请稍后重试");
        }
    }
}
