package com.trade.search.dto;

import com.trade.search.document.ProductDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索响应结果")
public class SearchResponse {

    @Schema(description = "总记录数", example = "1000")
    private Long total;

    @Schema(description = "当前页码", example = "0")
    private Integer page;

    @Schema(description = "每页大小", example = "10")
    private Integer size;

    @Schema(description = "总页数", example = "100")
    private Integer totalPages;

    @Schema(description = "是否还有下一页", example = "true")
    private Boolean hasNext;

    @Schema(description = "是否还有上一页", example = "false")
    private Boolean hasPrevious;

    @Schema(description = "商品列表（兼容旧接口）")
    private List<ProductDocument> products;

    @Schema(description = "商品列表（前端兼容）")
    private List<ProductDocument> records;

    @Schema(description = "搜索耗时（毫秒）", example = "25")
    private Long took;

    /**
     * 构建响应，自动同步 products 和 records
     */
    public static SearchResponse build(Long total, Integer page, Integer size,
                                        Integer totalPages, Boolean hasNext, Boolean hasPrevious,
                                        List<ProductDocument> products, Long took) {
        return SearchResponse.builder()
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .products(products)
                .records(products)  // 前端兼容
                .took(took)
                .build();
    }
}
