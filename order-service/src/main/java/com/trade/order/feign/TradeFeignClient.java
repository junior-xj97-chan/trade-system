package com.trade.order.feign;

import com.trade.common.R;
import com.trade.common.entity.Trade;
import com.trade.order.feign.fallback.TradeFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 交易服务 Feign 客户端
 * fallbackFactory：当交易服务异常/熔断时，返回降级结果
 */
@FeignClient(name = "trade-service", path = "/trade", fallbackFactory = TradeFeignFallbackFactory.class)
public interface TradeFeignClient {

    /**
     * 执行交易（撮合）
     */
    @PostMapping("/execute")
    R<Trade> execute(@RequestBody TradeRequest request);

    /**
     * 根据交易单号查询
     */
    @GetMapping("/no/{tradeNo}")
    R<Trade> getByTradeNo(@PathVariable("tradeNo") String tradeNo);

    /**
     * 退款交易
     */
    @PostMapping("/refund")
    R<Trade> refund(@RequestParam("orderId") Long orderId,
                    @RequestParam("userId") Long userId,
                    @RequestParam("productId") Long productId,
                    @RequestParam("price") java.math.BigDecimal price,
                    @RequestParam("quantity") Integer quantity);

    // ---- Request DTO ----
    @lombok.Data
    class TradeRequest {
        private Long orderId;
        private Long userId;
        private Long productId;
        private java.math.BigDecimal price;
        private Integer quantity;
        private Integer direction; // 1:买入 2:卖出
    }
}
