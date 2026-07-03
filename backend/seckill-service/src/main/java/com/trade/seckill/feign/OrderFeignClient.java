package com.trade.seckill.feign;

import com.trade.common.R;
import com.trade.seckill.dto.CreateOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", contextId = "seckillOrderFeignClient")
public interface OrderFeignClient {

    @PostMapping("/order/create")
    R<Long> createOrder(@RequestBody CreateOrderRequest request);
}
