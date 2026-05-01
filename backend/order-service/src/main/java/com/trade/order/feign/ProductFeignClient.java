package com.trade.order.feign;

import com.trade.common.R;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", contextId = "productFeignClient")
public interface ProductFeignClient {

    @GetMapping("/product/{productId}")
    R<ProductDTO> getById(@PathVariable("productId") Long productId);

    @Data
    class ProductDTO {
        private Long id;
        private String productCode;
        private String productName;
        private String market;   // SH/SZ/HK/US
        private Integer status;  // 1=正常 0=停牌
    }
}
