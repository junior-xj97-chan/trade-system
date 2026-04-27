package com.trade.search.repository;

import com.trade.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ES 商品文档仓库
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * 根据商品名称搜索
     */
    Page<ProductDocument> findByProductNameContaining(String keyword, Pageable pageable);

    /**
     * 根据商品代码搜索
     */
    ProductDocument findByProductCode(String productCode);

    /**
     * 根据交易所查询
     */
    List<ProductDocument> findByExchangeCode(String exchangeCode);

    /**
     * 根据商品类型查询
     */
    List<ProductDocument> findByProductType(String productType);

    /**
     * 自定义模糊搜索（支持多字段）
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"productName^3\", \"productCode^2\", \"description\"], \"type\": \"best_fields\", \"fuzziness\": \"AUTO\"}}")
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
}
