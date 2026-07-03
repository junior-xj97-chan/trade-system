package com.trade.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.seckill.entity.SeckillActivity;
import com.trade.seckill.entity.SeckillGoods;
import com.trade.seckill.mapper.SeckillActivityMapper;
import com.trade.seckill.mapper.SeckillGoodsMapper;
import com.trade.seckill.service.SeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl implements SeckillActivityService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillGoodsMapper goodsMapper;

    @Override
    public List<SeckillActivity> listActivities() {
        return activityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>()
                .orderByDesc(SeckillActivity::getStartDate)
        );
    }

    @Override
    public List<SeckillGoods> listGoodsByActivity(Long activityId) {
        return goodsMapper.selectList(
            new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
        );
    }

    @Override
    public SeckillActivity getActivityById(Long activityId) {
        return activityMapper.selectById(activityId);
    }
}
