package com.trade.seckill.controller;

import com.trade.common.R;
import com.trade.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀核心接口")
public class SeckillController {

    private final SeckillService seckillService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/path/{activityId}/{goodsId}")
    @Operation(summary = "获取秒杀路径")
    public R<String> getPath(@PathVariable Long activityId,
                             @PathVariable Long goodsId,
                             HttpServletRequest request) {
        Long userId = getUserId(request);
        String path = seckillService.getSeckillPath(activityId, goodsId, userId);
        return R.ok(path);
    }

    @PostMapping("/execute/{activityId}/{goodsId}")
    @Operation(summary = "执行秒杀")
    public R<String> execute(@PathVariable Long activityId,
                             @PathVariable Long goodsId,
                             @RequestParam String path,
                             HttpServletRequest request) {
        Long userId = getUserId(request);

        // 验证秒杀路径（第2层防超卖）
        String pathKey = "seckill:activity:" + activityId + ":path:" + userId + ":" + goodsId;
        Object cachedPath = redisTemplate.opsForValue().get(pathKey);
        if (cachedPath == null || !cachedPath.toString().equals(path)) {
            return R.fail("秒杀路径无效或已过期，请重新获取");
        }
        redisTemplate.delete(pathKey);

        String result = seckillService.executeSeckill(activityId, goodsId, userId);
        return R.ok(result);
    }

    @GetMapping("/result/{activityId}/{goodsId}")
    @Operation(summary = "查询秒杀结果")
    public R<Integer> getResult(@PathVariable Long activityId,
                                @PathVariable Long goodsId,
                                HttpServletRequest request) {
        Long userId = getUserId(request);
        Integer status = seckillService.getSeckillResult(activityId, goodsId, userId);
        String msg;
        if (status == 1) msg = "待支付";
        else if (status == 2) msg = "已支付";
        else if (status == 3) msg = "超时作废";
        else if (status == -1) msg = "排队中";
        else msg = "未知状态";
        return R.ok(msg, status);
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null) {
            throw new RuntimeException("未获取到用户信息");
        }
        return Long.parseLong(userIdStr);
    }
}
