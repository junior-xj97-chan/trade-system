package com.trade.quote.service;

import com.trade.quote.vo.QuoteVO;

import java.util.List;

public interface QuoteService {

    QuoteVO getRealtimeQuote(String stockCode);

    List<QuoteVO> getDailyKline(String stockCode, String startDate, String endDate);

    List<QuoteVO> searchStocks(String keyword);

    void processPriceUpdate(String stockCode, double price, double changePercent, long timestamp);
}
