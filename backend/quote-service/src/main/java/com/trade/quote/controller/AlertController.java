package com.trade.quote.controller;

import com.trade.common.R;
import com.trade.quote.dto.PriceAlertDTO;
import com.trade.quote.service.AlertService;
import com.trade.quote.util.UserContext;
import com.trade.quote.vo.PriceAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
@Tag(name = "价格提醒", description = "价格提醒CRUD")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "获取提醒列表")
    public R<List<PriceAlertVO>> list() {
        return R.ok(alertService.listByUserId(UserContext.getUserId()));
    }

    @PostMapping
    @Operation(summary = "创建提醒")
    public R<Long> create(@Valid @RequestBody PriceAlertDTO dto) {
        return R.ok(alertService.create(UserContext.getUserId(), dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提醒")
    public R<Void> delete(@PathVariable Long id) {
        alertService.delete(UserContext.getUserId(), id);
        return R.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新提醒")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PriceAlertDTO dto) {
        alertService.update(UserContext.getUserId(), id, dto);
        return R.ok();
    }
}
