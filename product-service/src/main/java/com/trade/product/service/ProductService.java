package com.trade.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.trade.product.entity.Product;

import java.math.BigDecimal;

/**
 * 商品服务接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 根据商品代码查询
     */
    Product getByProductCode(String productCode);

    /**
     * 更新商品价格
     */
    boolean updatePrice(Long productId, BigDecimal newPrice);

    /**
     * 分页查询商品列表
     */
    Page<Product> pageList(Page<Product> page, String keyword, Integer category, Integer status);

    /**
     * 上架商品
     */
    boolean online(Long productId);

    /**
     * 下架商品
     */
    boolean offline(Long productId);
}