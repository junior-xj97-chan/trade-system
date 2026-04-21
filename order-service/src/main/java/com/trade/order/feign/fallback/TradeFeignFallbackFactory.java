package com.trade.order.feign.fallback;

import com.trade.common.R;
import com.trade.common.entity.Trade;
import com.trade.order.feign.TradeFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TradeFeignClient 熔断降级处理器
 * 当交易服务不可用时，返回降级结果并记录日志
 */
@Slf4j
@Component
public class TradeFeignFallbackFactory implements FallbackFactory<TradeFeignClient> {

    @Override
    public TradeFeignClient create(Throwable cause) {
        log.error("[TradeFeignClient] 服务熔断降级，原因: {}", cause.getMessage());

        return new TradeFeignClient() {

            @Override
            public R<Trade> execute(TradeRequest request) {
                log.warn("[TradeFeignClient#execute] 熔断降级 orderId={}", request.getOrderId());
                return R.fail("交易服务暂不可用，撮合交易失败，请稍后重试");
            }

            @Override
            public R<Trade> getByTradeNo(String tradeNo) {
                log.warn("[TradeFeignClient#getByTradeNo] 熔断降级 tradeNo={}", tradeNo);
                return R.fail("交易服务暂不可用，查询交易失败，请稍后重试");
            }

            @Override
            public R<Trade> refund(Long orderId, Long userId, Long productId, BigDecimal price, Integer quantity) {
                log.warn("[TradeFeignClient#refund] 熔断降级 orderId={}", orderId);
                return R.fail("交易服务暂不可用，退款交易记录创建失败，请稍后重试");
            }
        };
    }
}
