package com.trade.seckill.controller;

import com.trade.common.R;
import com.trade.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀支付接口")
public class SeckillPayController {

    private final SeckillService seckillService;

    @PostMapping("/pay/{orderId}")
    @Operation(summary = "支付秒杀订单")
    public R<Void> pay(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = getUserId(request);
        seckillService.paySeckillOrder(orderId, userId);
        return R.ok();
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null) {
            throw new RuntimeException("未获取到用户信息");
        }
        return Long.parseLong(userIdStr);
    }
}
