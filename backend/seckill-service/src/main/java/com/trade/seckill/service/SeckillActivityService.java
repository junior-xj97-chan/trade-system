package com.trade.seckill.service;

import com.trade.seckill.entity.SeckillActivity;
import com.trade.seckill.entity.SeckillGoods;
import java.util.List;

public interface SeckillActivityService {

    List<SeckillActivity> listActivities();

    List<SeckillGoods> listGoodsByActivity(Long activityId);

    SeckillActivity getActivityById(Long activityId);
}
