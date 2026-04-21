package com.trade.order.controller;

import com.trade.common.R;
import com.trade.order.entity.Order;
import com.trade.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public R<Order> create(@RequestBody CreateOrderRequest request) {
        return R.ok(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询订单")
    public R<Order> getById(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @GetMapping("/no/{orderNo}")
    @Operation(summary = "根据订单号查询")
    public R<Order> getByOrderNo(@PathVariable String orderNo) {
        return R.ok(orderService.getByOrderNo(orderNo));
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return R.ok();
    }

    @PutMapping("/pay/{id}")
    @Operation(summary = "支付订单（买入）")
    public R<Void> pay(@PathVariable Long id) {
        orderService.payOrder(id);
        return R.ok();
    }

    @PutMapping("/sell/{id}")
    @Operation(summary = "卖出订单（支付并收款）")
    public R<Void> sell(@PathVariable Long id) {
        orderService.sellOrder(id);
        return R.ok();
    }

    // ---- Request DTO ----
    @lombok.Data
    public static class CreateOrderRequest {
        private Long userId;
        private Long productId;
        private String productName;
        private java.math.BigDecimal price;
        private Integer quantity;
        private Integer direction; // 1:买入 2:卖出，默认买入
    }
}
