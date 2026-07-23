package com.trade.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.order.entity.Order;
import com.trade.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public R<Order> create(@RequestBody CreateOrderRequest request,
                           @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (request.getUserId() != null) {
            if (currentUserId != null && !currentUserId.equals(request.getUserId())) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        } else if (currentUserId != null) {
            request.setUserId(currentUserId);
        }
        return R.ok(orderService.createOrder(request));
    }

    // ========== 查询接口（注意：/page 必须在 /{id} 前面，否则路由冲突）==========

    @GetMapping("/page")
    @Operation(summary = "分页查询订单（支持按userId过滤）")
    public R<PageResult<Order>> page(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "用户ID（可选）") @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        Long queryUserId = resolveQueryUserId(userId, currentUserId);
        Page<Order> page = orderService.page(current, size, queryUserId);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }

    @GetMapping("/list")
    @Operation(summary = "查询用户全部订单")
    public R<List<Order>> getByUserId(@RequestParam(required = false) Long userId,
                                       @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        Long queryUserId = resolveQueryUserId(userId, currentUserId);
        return R.ok(orderService.getByUserId(queryUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单")
    public R<Order> getById(@PathVariable Long id,
                            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        Order order = orderService.getById(id);
        if (currentUserId != null && !currentUserId.equals(order.getUserId())) {
            throw new BusinessException(BizCode.NO_PERMISSION);
        }
        return R.ok(order);
    }

    @GetMapping("/no/{orderNo}")
    @Operation(summary = "根据订单号查询")
    public R<Order> getByOrderNo(@PathVariable String orderNo,
                                 @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        Order order = orderService.getByOrderNo(orderNo);
        if (currentUserId != null && !currentUserId.equals(order.getUserId())) {
            throw new BusinessException(BizCode.NO_PERMISSION);
        }
        return R.ok(order);
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public R<Void> cancel(@PathVariable Long id,
                          @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        checkOrderOwner(id, currentUserId);
        orderService.cancelOrder(id);
        return R.ok();
    }

    @PutMapping("/pay/{id}")
    @Operation(summary = "支付订单（买入）")
    public R<Void> pay(@PathVariable Long id,
                       @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        checkOrderOwner(id, currentUserId);
        orderService.payOrder(id);
        return R.ok();
    }

    @PutMapping("/sell/{id}")
    @Operation(summary = "卖出订单（支付并收款）")
    public R<Void> sell(@PathVariable Long id,
                        @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        checkOrderOwner(id, currentUserId);
        orderService.sellOrder(id);
        return R.ok();
    }

    private void checkOrderOwner(Long orderId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(BizCode.NO_PERMISSION);
        }
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (!currentUserId.equals(order.getUserId())) {
            throw new BusinessException(BizCode.NO_PERMISSION);
        }
    }

    private Long resolveQueryUserId(Long requestUserId, Long currentUserId) {
        if (currentUserId == null) {
            return requestUserId;
        }
        if (requestUserId != null && !requestUserId.equals(currentUserId)) {
            throw new BusinessException(BizCode.NO_PERMISSION);
        }
        return currentUserId;
    }

    // ---- Request DTO ----
    @lombok.Data
    public static class CreateOrderRequest {
        private Long userId;
        private Long productId;
        private String productName;
        private String productCode;    // 商品代码（股票代码）
        private java.math.BigDecimal price;
        private Integer quantity;
        private Integer direction; // 1:买入 2:卖出，默认买入
        private Integer source;    // 1:普通订单 2:秒杀订单
    }
}
