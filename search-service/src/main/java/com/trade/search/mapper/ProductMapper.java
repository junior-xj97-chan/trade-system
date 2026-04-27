package com.trade.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.entity.ProductDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Object> {

    /**
     * 分页查询商品列表
     */
    @Select("SELECT id, product_code as productCode, product_name as productName, " +
            "current_price as currentPrice, category, status, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM t_product WHERE deleted = 0 AND status = 1")
    IPage<ProductDTO> selectProductPage(Page<Object> page);
}
