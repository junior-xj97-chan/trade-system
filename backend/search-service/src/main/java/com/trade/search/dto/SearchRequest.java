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

    @Schema(description = "市场代码（SH、SZ、HK、US），与 exchangeCode 相同", example = "SH")
    private String market;

    @Schema(description = "交易所代码（SH、SZ、HK、US）", example = "SH")
    private String exchangeCode;

    @Schema(description = "商品类型（stock、fund、bond、future）", example = "stock")
    private String productType;

    @Schema(description = "最低价格")
    private Double minPrice;

    @Schema(description = "最高价格")
    private Double maxPrice;

    @Schema(description = "排序字段（price、changePercent）", example = "price")
    private String sortField;

    @Schema(description = "排序方向（asc、desc）", example = "desc")
    private String sortOrder;

    @Schema(description = "排序字段简写（price、changePercent），前端传入格式为 price_asc", example = "price")
    private String sortBy;

    @Schema(description = "页码（从 0 开始）", example = "0")
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "每页大小", example = "10")
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 10;

    @Schema(description = "每页大小（别名）", example = "10")
    private Integer pageSize;

    /**
     * 获取最终使用的市场代码
     * 优先使用 exchangeCode，否则用 market
     */
    public String getExchangeCodeEffective() {
        if (exchangeCode != null && !exchangeCode.isBlank()) {
            return exchangeCode;
        }
        return market;
    }

    /**
     * 获取最终使用的排序字段
     * 优先使用 sortField，否则从 sortBy 解析
     */
    public String getSortFieldEffective() {
        if (sortField != null && !sortField.isBlank()) {
            return sortField;
        }
        if (sortBy != null && sortBy.contains("_")) {
            return sortBy.split("_")[0];
        }
        return sortBy;
    }

    /**
     * 获取最终使用的排序方向
     */
    public String getSortOrderEffective() {
        if (sortOrder != null && !sortOrder.isBlank()) {
            return sortOrder;
        }
        if (sortBy != null && sortBy.contains("_")) {
            String[] parts = sortBy.split("_");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return sortOrder;
    }

    /**
     * 获取最终使用的分页大小
     */
    public Integer getSizeEffective() {
        if (pageSize != null && pageSize > 0) {
            return pageSize;
        }
        return size;
    }
}
