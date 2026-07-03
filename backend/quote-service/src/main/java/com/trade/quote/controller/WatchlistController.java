package com.trade.quote.controller;

import com.trade.common.R;
import com.trade.quote.dto.WatchlistDTO;
import com.trade.quote.service.WatchlistService;
import com.trade.quote.util.UserContext;
import com.trade.quote.vo.WatchlistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
@Tag(name = "自选股管理", description = "自选股CRUD、标签管理")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    @Operation(summary = "获取自选股列表")
    public R<List<WatchlistVO>> list() {
        return R.ok(watchlistService.listByUserId(UserContext.getUserId()));
    }

    @PostMapping("/add")
    @Operation(summary = "添加自选")
    public R<Long> add(@Valid @RequestBody WatchlistDTO dto) {
        return R.ok(watchlistService.add(UserContext.getUserId(), dto));
    }

    @DeleteMapping("/remove/{id}")
    @Operation(summary = "删除自选")
    public R<Void> remove(@PathVariable Long id) {
        watchlistService.remove(UserContext.getUserId(), id);
        return R.ok();
    }

    @PutMapping("/tags/{id}")
    @Operation(summary = "更新标签")
    public R<Void> updateTags(@PathVariable Long id, @RequestParam String tags) {
        watchlistService.updateTags(UserContext.getUserId(), id, tags);
        return R.ok();
    }

    @PutMapping("/note/{id}")
    @Operation(summary = "更新备注")
    public R<Void> updateNote(@PathVariable Long id, @RequestParam String note) {
        watchlistService.updateNote(UserContext.getUserId(), id, note);
        return R.ok();
    }
}
