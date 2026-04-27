package com.trade.product.feign;

import com.trade.common.R;
import com.trade.common.entity.ProductDTO;
import com.trade.product.feign.fallback.ProductFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端
 * 供其他服务查询商品信息
 */
@FeignClient(name = "product-service", path = "/product", fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    /**
     * 查询商品详情（根据ID）
     */
    @GetMapping("/{productId}")
    R<ProductDTO> getById(@PathVariable("productId") Long productId);

    /**
     * 查询商品详情（根据商品代码）
     */
    @GetMapping("/code/{productCode}")
    R<ProductDTO> getByCode(@PathVariable("productCode") String productCode);
}
