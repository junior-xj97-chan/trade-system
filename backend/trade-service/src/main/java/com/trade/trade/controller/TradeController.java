package com.trade.trade.controller;

import com.trade.common.R;
import com.trade.trade.entity.Trade;
import com.trade.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
@Tag(name = "交易管理")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/execute")
    @Operation(summary = "执行交易（撮合）")
    public R<Trade> execute(@RequestBody TradeRequest request) {
        return R.ok(tradeService.executeTrade(request));
    }

    @PostMapping("/refund")
    @Operation(summary = "退款交易")
    public R<Trade> refund(@RequestParam Long orderId,
                           @RequestParam Long userId,
                           @RequestParam Long productId,
                           @RequestParam java.math.BigDecimal price,
                           @RequestParam Integer quantity) {
        return R.ok(tradeService.refundTrade(orderId, userId, productId, price, quantity));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询交易")
    public R<Trade> getById(@PathVariable Long id) {
        return R.ok(tradeService.getById(id));
    }

    @GetMapping("/no/{tradeNo}")
    @Operation(summary = "根据交易单号查询")
    public R<Trade> getByTradeNo(@PathVariable String tradeNo) {
        return R.ok(tradeService.getByTradeNo(tradeNo));
    }

    @lombok.Data
    public static class TradeRequest {
        private Long orderId;
        private Long userId;
        private Long productId;
        private java.math.BigDecimal price;
        private Integer quantity;
        private Integer direction; // 1:买入 2:卖出
    }
}
