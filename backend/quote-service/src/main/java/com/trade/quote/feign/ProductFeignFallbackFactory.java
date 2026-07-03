package com.trade.quote.feign;

import com.trade.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("ProductFeignClient fallback triggered: {}", cause.getMessage());
        return new ProductFeignClient() {
            @Override
            public R<ProductFeignClient.ProductDTO> getByCode(String productCode) {
                return R.fail("Product service unavailable");
            }
        };
    }
}
