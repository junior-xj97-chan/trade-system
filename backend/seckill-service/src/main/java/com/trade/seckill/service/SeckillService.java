package com.trade.seckill.service;

import com.trade.seckill.dto.SeckillMessage;
import com.trade.seckill.entity.SeckillOrder;

public interface SeckillService {

    String getSeckillPath(Long activityId, Long goodsId, Long userId);

    String executeSeckill(Long activityId, Long goodsId, Long userId);

    Integer getSeckillResult(Long activityId, Long goodsId, Long userId);

    void processSeckillOrder(SeckillMessage message);

    void paySeckillOrder(Long orderId, Long userId);

    void cancelTimeoutOrder(Long orderId);
}
