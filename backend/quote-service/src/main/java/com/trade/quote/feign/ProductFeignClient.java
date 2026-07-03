package com.trade.quote.feign;

import com.trade.common.R;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "product-service", contextId = "quoteProductFeignClient")
public interface ProductFeignClient {

    @GetMapping("/product/code/{productCode}")
    R<ProductDTO> getByCode(@PathVariable("productCode") String productCode);

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
