package com.trade.seckill.feign;

import com.trade.common.R;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@FeignClient(name = "product-service", contextId = "seckillProductFeignClient")
public interface ProductFeignClient {

    @GetMapping("/product/{productId}")
    R<ProductDTO> getById(@PathVariable("productId") Long productId);

    @Data
    class ProductDTO {
        private Long id;
        private String productCode;
        private String productName;
        private String market;
        private Integer category;
        private Integer status;
        private BigDecimal currentPrice;
    }
}
