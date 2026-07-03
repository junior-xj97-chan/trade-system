package com.trade.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.seckill.entity.SeckillActivity;
import com.trade.seckill.entity.SeckillGoods;
import com.trade.seckill.mapper.SeckillActivityMapper;
import com.trade.seckill.mapper.SeckillGoodsMapper;
import com.trade.seckill.service.SeckillPreheatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPreheatServiceImpl implements SeckillPreheatService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillGoodsMapper goodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${seckill.preheat-before-start-minutes}")
    private int preheatBeforeStartMinutes;

    @Override
    public void preheatActivity(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("【库存预热】活动不存在: activityId={}", activityId);
            return;
        }
        if (activity.getPreheated() == 1) {
            log.info("【库存预热】活动已预热，跳过: activityId={}", activityId);
            return;
        }

        List<SeckillGoods> goodsList = goodsMapper.selectList(
            new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
        );

        int batchSize = 100;
        for (int i = 0; i < goodsList.size(); i += batchSize) {
            List<SeckillGoods> batch = goodsList.subList(i,
                Math.min(i + batchSize, goodsList.size()));
            for (SeckillGoods goods : batch) {
                String stockKey = "seckill:activity:" + activityId + ":stock:" + goods.getId();
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                    stockKey,
                    String.valueOf(goods.getStockCount()),
                    Duration.ofDays(7)
                );
                if (Boolean.TRUE.equals(isNew)) {
                    log.info("【库存预热】预热成功: activityId={}, goodsId={}, stock={}",
                        activityId, goods.getId(), goods.getStockCount());
                } else {
                    log.warn("【库存预热】库存 Key 已存在，跳过: activityId={}, goodsId={}",
                        activityId, goods.getId());
                }
            }
        }

        activity.setPreheated(1);
        activityMapper.updateById(activity);
        log.info("【库存预热】活动预热完成: activityId={}", activityId);
    }

    @Override
    public void autoPreheatScheduled() {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(preheatBeforeStartMinutes);
        List<SeckillActivity> activities = activityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 0)
                .eq(SeckillActivity::getPreheated, 0)
                .le(SeckillActivity::getStartDate, deadline)
        );

        for (SeckillActivity activity : activities) {
            try {
                preheatActivity(activity.getId());
            } catch (Exception e) {
                log.error("【自动预热】失败: activityId={}", activity.getId(), e);
            }
        }
    }
}
