package com.trade.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trade.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Update;

public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    @Update("UPDATE seckill_goods SET stock_count = stock_count - 1 WHERE id = #{goodsId} AND stock_count > 0")
    int deductStock(Long goodsId);
}
