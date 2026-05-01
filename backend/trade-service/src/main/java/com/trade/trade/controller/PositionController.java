package com.trade.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.trade.entity.Position;
import com.trade.trade.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 持仓管理控制器
 * 路径：/trade/position/**（与 Gateway 路由 /api/trade/** 配合）
 */
@RestController
@RequestMapping("/trade/position")
@RequiredArgsConstructor
@Tag(name = "持仓管理")
public class PositionController {

    private final PositionService positionService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户持仓列表")
    public R<List<Position>> getByUserId(@PathVariable Long userId) {
        return R.ok(positionService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/product/{productId}")
    @Operation(summary = "查询用户单只股票持仓")
    public R<Position> getByUserIdAndProductId(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        Position position = positionService.getByUserIdAndProductId(userId, productId);
        return R.ok(position);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询所有持仓")
    public R<PageResult<Position>> page(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size) {
        Page<Position> page = positionService.page(current, size);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal(), current, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询持仓")
    public R<Position> getById(@PathVariable Long id) {
        return R.ok(positionService.getById(id));
    }

    @PostMapping("/updatePrice")
    @Operation(summary = "更新持仓价格（行情更新时调用）")
    public R<Void> updatePrice(
            @RequestParam Long productId,
            @RequestParam BigDecimal newPrice) {
        positionService.updatePrice(productId, newPrice);
        return R.ok();
    }

    @PostMapping("/buy")
    @Operation(summary = "买入建仓/加仓（内部接口，由订单服务调用）")
    public R<Void> buy(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam String productName,
            @RequestParam String productCode,
            @RequestParam Integer quantity,
            @RequestParam java.math.BigDecimal price) {
        positionService.buy(orderId, userId, productId, productName, productCode, quantity, price);
        return R.ok();
    }

    @PostMapping("/sell")
    @Operation(summary = "卖出减仓（内部接口，由订单服务调用）")
    public R<Void> sell(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam java.math.BigDecimal price) {
        positionService.sell(orderId, userId, productId, quantity, price);
        return R.ok();
    }
}
