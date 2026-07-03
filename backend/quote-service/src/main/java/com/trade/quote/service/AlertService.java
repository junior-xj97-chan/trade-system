package com.trade.quote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.trade.quote.dto.PriceAlertDTO;
import com.trade.quote.entity.PriceAlert;
import com.trade.quote.vo.PriceAlertVO;

import java.util.List;

public interface AlertService extends IService<PriceAlert> {

    List<PriceAlertVO> listByUserId(Long userId);

    Long create(Long userId, PriceAlertDTO dto);

    void delete(Long userId, Long id);

    void update(Long userId, Long id, PriceAlertDTO dto);

    void checkAndTrigger(String stockCode, double currentPrice);
}
