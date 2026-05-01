package com.trade.product.feign.fallback;

import com.trade.common.R;
import com.trade.common.entity.ProductDTO;
import com.trade.product.feign.ProductFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * ProductFeignClient 降级工厂
 * 当 product-service 不可用时，返回降级结果
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("【Feign降级】ProductFeignClient 调用失败，原因: {}", cause.getMessage());
        return new ProductFeignClient() {
            @Override
            public R<ProductDTO> getById(Long productId) {
                log.warn("【Feign降级】getById productId={}", productId);
                return R.fail("商品服务暂时不可用");
            }

            @Override
            public R<ProductDTO> getByCode(String productCode) {
                log.warn("【Feign降级】getByCode productCode={}", productCode);
                return R.fail("商品服务暂时不可用");
            }
        };
    }
}
