package com.trade.search.controller;

import com.trade.search.document.ProductDocument;
import com.trade.search.dto.SearchRequest;
import com.trade.search.dto.SearchResponse;
import com.trade.search.service.DataSyncService;
import com.trade.search.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索接口
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "商品搜索", description = "基于 Elasticsearch 的商品搜索服务")
public class SearchController {

    private final ProductSearchService productSearchService;
    private final DataSyncService dataSyncService;

    /**
     * 高级搜索接口
     */
    @Operation(summary = "高级搜索", description = "支持关键词、交易所、类型等多条件搜索")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        log.info("搜索请求: keyword={}, exchange={}, type={}, page={}, size={}",
                request.getKeyword(), request.getExchangeCode(), request.getProductType(),
                request.getPage(), request.getSize());

        SearchResponse response = productSearchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 快速搜索接口（关键词搜索）
     */
    @Operation(summary = "快速搜索", description = "基于关键词的快速搜索，返回分页结果")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class)))
    })
    @GetMapping("/quick")
    public ResponseEntity<SearchResponse> quickSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        log.info("快速搜索: keyword={}, page={}, size={}", keyword, page, size);

        SearchResponse response = productSearchService.quickSearch(keyword, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据商品代码查询
     */
    @Operation(summary = "按代码查询", description = "根据商品代码精确查询单个商品")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    @GetMapping("/code/{productCode}")
    public ResponseEntity<ProductDocument> findByCode(
            @Parameter(description = "商品代码，如 600000.SH") @PathVariable String productCode) {
        log.info("查询商品代码: {}", productCode);

        ProductDocument product = productSearchService.findByCode(productCode);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * 索引单个商品
     */
    @Operation(summary = "索引单个商品", description = "将单个商品写入 Elasticsearch 索引")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "索引成功",
                    content = @Content(schema = @Schema(implementation = ProductDocument.class)))
    })
    @PostMapping("/index")
    public ResponseEntity<ProductDocument> indexProduct(@RequestBody ProductDocument product) {
        log.info("索引商品: {}", product.getProductCode());

        ProductDocument indexed = productSearchService.indexProduct(product);
        return ResponseEntity.ok(indexed);
    }

    /**
     * 批量索引商品
     */
    @Operation(summary = "批量索引商品", description = "将多个商品批量写入 Elasticsearch 索引")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "索引成功")
    })
    @PostMapping("/index/batch")
    public ResponseEntity<Void> indexProducts(@RequestBody List<ProductDocument> products) {
        log.info("批量索引商品，数量: {}", products.size());

        productSearchService.indexProducts(products);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除商品索引
     */
    @Operation(summary = "删除商品索引", description = "根据商品ID从 Elasticsearch 中删除索引")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功")
    })
    @DeleteMapping("/index/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        log.info("删除商品索引: {}", id);

        productSearchService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 检查索引状态
     */
    @Operation(summary = "检查索引状态", description = "检查 Elasticsearch 索引是否存在")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回索引是否存在")
    })
    @GetMapping("/status")
    public ResponseEntity<Boolean> checkIndexExists() {
        boolean exists = productSearchService.indexExists();
        return ResponseEntity.ok(exists);
    }

    /**
     * 创建索引
     */
    @Operation(summary = "创建索引", description = "创建商品搜索索引（如果不存在）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    @PostMapping("/index/create")
    public ResponseEntity<Void> createIndex() {
        log.info("创建索引");
        productSearchService.createIndex();
        return ResponseEntity.ok().build();
    }

    /**
     * 全量同步 MySQL 数据到 ES
     */
    @Operation(summary = "全量同步", description = "将 MySQL 中的所有商品数据同步到 Elasticsearch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "同步完成")
    })
    @PostMapping("/index/full")
    public ResponseEntity<Map<String, String>> fullSync() {
        log.info("手动触发全量同步");
        dataSyncService.manualFullSync();
        return ResponseEntity.ok(Map.of("message", "全量同步任务已触发，请查看控制台日志"));
    }

    /**
     * 增量同步 MySQL 数据到 ES
     */
    @Operation(summary = "增量同步", description = "将 MySQL 中最近更新的商品数据同步到 Elasticsearch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "同步完成")
    })
    @PostMapping("/index/incremental")
    public ResponseEntity<Map<String, String>> incrementalSync() {
        log.info("手动触发增量同步");
        dataSyncService.incrementalSync();
        return ResponseEntity.ok(Map.of("message", "增量同步任务已触发，请查看控制台日志"));
    }
}
