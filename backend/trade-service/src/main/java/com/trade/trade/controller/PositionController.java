package com.trade.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.PageResult;
import com.trade.common.R;
import com.trade.common.config.FeignInternalInterceptor;
import com.trade.trade.entity.Position;
import com.trade.trade.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final HttpServletRequest request;

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户持仓列表")
    public R<List<Position>> getByUserId(@PathVariable Long userId,
                                         @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (!isInternalService()) {
            if (currentUserId == null) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        }
        return R.ok(positionService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/product/{productId}")
    @Operation(summary = "查询用户单只股票持仓")
    public R<Position> getByUserIdAndProductId(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        if (!isInternalService()) {
            if (currentUserId == null) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        }
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
    public R<Position> getById(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        Position position = positionService.getById(id);
        if (!isInternalService() && currentUserId != null) {
            if (!currentUserId.equals(position.getUserId())) {
                throw new BusinessException(BizCode.NO_PERMISSION);
            }
        }
        return R.ok(position);
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
        assertInternalService();
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
        assertInternalService();
        positionService.sell(orderId, userId, productId, quantity, price);
        return R.ok();
    }

    private boolean isInternalService() {
        String header = request.getHeader(FeignInternalInterceptor.INTERNAL_HEADER);
        return FeignInternalInterceptor.INTERNAL_HEADER_VALUE.equals(header);
    }

    private void assertInternalService() {
        if (!isInternalService()) {
            throw new BusinessException(BizCode.INTERNAL_ACCESS_DENIED);
        }
    }
}
