package com.trade.seckill.controller;

import com.trade.common.R;
import com.trade.seckill.entity.SeckillActivity;
import com.trade.seckill.entity.SeckillGoods;
import com.trade.seckill.service.SeckillActivityService;
import com.trade.seckill.service.SeckillPreheatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
@Tag(name = "秒杀活动管理")
public class SeckillActivityController {

    private final SeckillActivityService activityService;
    private final SeckillPreheatService preheatService;

    @GetMapping("/activities")
    @Operation(summary = "活动列表")
    public R<List<SeckillActivity>> listActivities() {
        return R.ok(activityService.listActivities());
    }

    @GetMapping("/goods/{activityId}")
    @Operation(summary = "活动商品列表")
    public R<List<SeckillGoods>> listGoods(@PathVariable Long activityId) {
        return R.ok(activityService.listGoodsByActivity(activityId));
    }

    @PostMapping("/preheat/{activityId}")
    @Operation(summary = "手动预热库存")
    public R<Void> preheat(@PathVariable Long activityId) {
        preheatService.preheatActivity(activityId);
        return R.ok();
    }
}
