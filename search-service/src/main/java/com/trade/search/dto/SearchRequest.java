package com.trade.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索请求参数")
public class SearchRequest {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "交易所代码（SH、SZ、HK、US）", example = "SH")
    private String exchangeCode;

    @Schema(description = "商品类型（stock、fund、bond、future）", example = "stock")
    private String productType;

    @Schema(description = "排序字段（price、changePercent、volume、amount）", example = "price")
    private String sortField;

    @Schema(description = "排序方向（asc、desc）", example = "desc")
    private String sortOrder;

    @Schema(description = "页码（从 0 开始）", example = "0")
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "每页大小", example = "10")
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 10;
}
