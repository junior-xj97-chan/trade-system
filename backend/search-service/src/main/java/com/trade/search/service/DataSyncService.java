package com.trade.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.entity.ProductDTO;
import com.trade.search.document.ProductDocument;
import com.trade.search.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据同步服务
 * 从 MySQL 同步商品数据到 Elasticsearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final ProductMapper productMapper;
    private final ProductSearchService productSearchService;

    /**
     * 全量同步
     */
    public void fullSync() {
        log.info("开始全量同步商品数据到 ES...");
        long startTime = System.currentTimeMillis();

        try {
            // 检查索引是否存在，不存在则创建
            if (!productSearchService.indexExists()) {
                productSearchService.createIndex();
                log.info("创建 ES 索引成功");
            }

            // 分页查询所有正常状态的商品
            int pageNum = 1;
            int pageSize = 500;
            int totalSynced = 0;

            while (true) {
                Page<Object> page = new Page<>(pageNum, pageSize);
                var result = productMapper.selectProductPage(page);

                if (result.getRecords().isEmpty()) {
                    break;
                }

                // 转换为 ES 文档
                List<ProductDocument> documents = new ArrayList<>();
                for (ProductDTO product : result.getRecords()) {
                    documents.add(convertToDocument(product));
                }

                // 批量索引
                productSearchService.indexProducts(documents);
                totalSynced += documents.size();

                if (result.getCurrent() >= result.getPages()) {
                    break;
                }
                pageNum++;
            }

            long took = System.currentTimeMillis() - startTime;
            log.info("全量同步完成，共同步 {} 条记录，耗时 {} ms", totalSynced, took);
        } catch (Exception e) {
            log.error("全量同步失败", e);
        }
    }

    /**
     * 增量同步
     */
    public void incrementalSync() {
        log.info("开始增量同步商品数据...");

        try {
            // 检查索引是否存在
            if (!productSearchService.indexExists()) {
                log.info("ES 索引不存在，跳过增量同步");
                return;
            }

            // 分页查询商品
            int pageNum = 1;
            int pageSize = 500;
            int totalSynced = 0;

            while (true) {
                Page<Object> page = new Page<>(pageNum, pageSize);
                var result = productMapper.selectProductPage(page);

                if (result.getRecords().isEmpty()) {
                    break;
                }

                // 转换为 ES 文档
                List<ProductDocument> documents = new ArrayList<>();
                for (ProductDTO product : result.getRecords()) {
                    documents.add(convertToDocument(product));
                }

                // 批量索引
                productSearchService.indexProducts(documents);
                totalSynced += documents.size();

                if (result.getCurrent() >= result.getPages()) {
                    break;
                }
                pageNum++;
            }

            log.info("增量同步完成，共同步 {} 条记录", totalSynced);
        } catch (Exception e) {
            log.error("增量同步失败", e);
        }
    }

    /**
     * 手动触发全量同步
     */
    public void manualFullSync() {
        fullSync();
    }

    /**
     * 转换为 ES 文档
     */
    private ProductDocument convertToDocument(ProductDTO product) {
        return ProductDocument.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .currentPrice(product.getCurrentPrice())
                .productType(convertCategoryToType(product.getCategory()))
                .exchangeCode(extractExchange(product.getProductCode()))
                .status(product.getStatus())
                .updateTime(product.getUpdateTime())
                .createTime(product.getCreateTime())
                .build();
    }

    /**
     * 将分类转换为商品类型
     */
    private String convertCategoryToType(Integer category) {
        if (category == null) return "other";
        return switch (category) {
            case 1 -> "stock";
            case 2 -> "fund";
            case 3 -> "future";
            default -> "other";
        };
    }

    /**
     * 从商品代码提取交易所代码
     */
    private String extractExchange(String productCode) {
        if (productCode == null || productCode.isEmpty()) {
            return "OTHER";
        }
        if (productCode.endsWith(".SH") || productCode.endsWith(".SZ")) {
            return productCode.substring(productCode.length() - 2);
        }
        // 默认根据代码规则判断
        if (productCode.startsWith("6")) {
            return "SH";
        } else if (productCode.startsWith("0") || productCode.startsWith("3")) {
            return "SZ";
        }
        return "OTHER";
    }
}
