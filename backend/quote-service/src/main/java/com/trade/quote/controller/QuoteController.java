package com.trade.quote.controller;

import com.trade.common.R;
import com.trade.quote.service.QuoteService;
import com.trade.quote.vo.QuoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quote")
@RequiredArgsConstructor
@Tag(name = "行情查询", description = "实时行情、历史K线、股票搜索")
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping("/realtime")
    @Operation(summary = "获取实时行情")
    public R<QuoteVO> getRealtime(@RequestParam String code) {
        return R.ok(quoteService.getRealtimeQuote(code));
    }

    @GetMapping("/daily")
    @Operation(summary = "获取历史K线")
    public R<List<QuoteVO>> getDaily(
            @RequestParam String code,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(quoteService.getDailyKline(code, startDate, endDate));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索股票")
    public R<List<QuoteVO>> search(@RequestParam String keyword) {
        return R.ok(quoteService.searchStocks(keyword));
    }
}
