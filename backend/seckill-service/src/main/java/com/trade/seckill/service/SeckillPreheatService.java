package com.trade.seckill.service;

public interface SeckillPreheatService {

    void preheatActivity(Long activityId);

    void autoPreheatScheduled();
}
