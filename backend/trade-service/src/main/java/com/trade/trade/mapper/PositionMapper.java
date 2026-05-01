package com.trade.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trade.trade.entity.Position;
import org.apache.ibatis.annotations.Mapper;

/**
 * 持仓Mapper
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {
}
