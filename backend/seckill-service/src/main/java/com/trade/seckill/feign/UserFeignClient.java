package com.trade.seckill.feign;

import com.trade.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "seckillUserFeignClient")
public interface UserFeignClient {

    @GetMapping("/user/{id}")
    R<UserDTO> getById(@PathVariable("id") Long id);

    class UserDTO {
        private Long id;
        private String username;
        private String phone;
    }
}
