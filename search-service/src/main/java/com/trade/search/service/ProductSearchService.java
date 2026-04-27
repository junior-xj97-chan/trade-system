package com.trade.search.service;

import com.trade.search.document.ProductDocument;
import com.trade.search.dto.SearchRequest;
import com.trade.search.dto.SearchResponse;
import com.trade.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

/**
 * 商品搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 高级搜索（支持多条件组合）
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        // 构建分页
        Pageable pageable = buildPageable(request);

        // 构建查询
        Query query = buildQuery(request);

        // 执行搜索
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

        // 转换为响应
        List<ProductDocument> products = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        long total = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) total / request.getSize());

        long took = System.currentTimeMillis() - startTime;

        return SearchResponse.builder()
                .total(total)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages(totalPages)
                .hasNext(request.getPage() < totalPages - 1)
                .hasPrevious(request.getPage() > 0)
                .products(products)
                .took(took)
                .build();
    }

    /**
     * 关键词快速搜索
     */
    public SearchResponse quickSearch(String keyword, int page, int size) {
        long startTime = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDocument> result = productSearchRepository.searchByKeyword(keyword, pageable);

        long took = System.currentTimeMillis() - startTime;

        return SearchResponse.builder()
                .total(result.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .products(result.getContent())
                .took(took)
                .build();
    }

    /**
     * 根据商品代码精确查询
     */
    public ProductDocument findByCode(String productCode) {
        return productSearchRepository.findByProductCode(productCode);
    }

    /**
     * 索引单个商品文档
     */
    public ProductDocument indexProduct(ProductDocument product) {
        log.info("索引商品: {}, {}", product.getProductCode(), product.getProductName());
        return productSearchRepository.save(product);
    }

    /**
     * 批量索引商品
     */
    public void indexProducts(List<ProductDocument> products) {
        log.info("批量索引商品，数量: {}", products.size());
        productSearchRepository.saveAll(products);
    }

    /**
     * 删除商品索引
     */
    public void deleteProduct(Long id) {
        log.info("删除商品索引: {}", id);
        productSearchRepository.deleteById(id);
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists() {
        return elasticsearchOperations.indexOps(ProductDocument.class).exists();
    }

    /**
     * 创建索引
     */
    public void createIndex() {
        elasticsearchOperations.indexOps(ProductDocument.class).create();
        log.info("创建 ES 索引成功");
    }

    /**
     * 构建分页
     */
    private Pageable buildPageable(SearchRequest request) {
        Sort sort = Sort.unsorted();

        if (request.getSortField() != null && request.getSortOrder() != null) {
            Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortOrder())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, convertSortField(request.getSortField()));
        }

        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    /**
     * 转换排序字段
     */
    private String convertSortField(String field) {
        return switch (field) {
            case "price" -> "currentPrice";
            case "change" -> "changePercent";
            case "vol" -> "volume";
            default -> field;
        };
    }

    /**
     * 构建查询
     */
    private Query buildQuery(SearchRequest request) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 关键词搜索
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            boolQueryBuilder.must(QueryBuilders.multiMatch()
                    .query(request.getKeyword())
                    .fields("productName^3", "productCode^2", "description")
                    .fuzziness("AUTO")
                    .build()._toQuery());
        }

        // 交易所过滤
        if (request.getExchangeCode() != null && !request.getExchangeCode().isBlank()) {
            boolQueryBuilder.filter(QueryBuilders.term()
                    .field("exchangeCode")
                    .value(request.getExchangeCode())
                    .build()._toQuery());
        }

        // 商品类型过滤
        if (request.getProductType() != null && !request.getProductType().isBlank()) {
            boolQueryBuilder.filter(QueryBuilders.term()
                    .field("productType")
                    .value(request.getProductType())
                    .build()._toQuery());
        }

        return NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withPageable(buildPageable(request))
                .build();
    }
}
