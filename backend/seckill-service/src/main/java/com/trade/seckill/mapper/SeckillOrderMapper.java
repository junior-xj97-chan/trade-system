package com.trade.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trade.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Update;

public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    @Update("UPDATE seckill_order SET status = 3 WHERE id = #{id} AND status = 1")
    int cancelTimeoutOrder(Long id);
}
