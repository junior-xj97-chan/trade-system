package com.trade.search.consumer;

import com.trade.common.R;
import com.trade.common.config.RabbitMQConfig;
import com.trade.common.entity.ProductDTO;
import com.trade.common.mq.ProductSyncMessage;
import com.trade.search.document.ProductDocument;
import com.trade.search.feign.ProductFeignClient;
import com.trade.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品同步 MQ 消费者
 * 监听 product-service 发送的商品变更消息，同步到 Elasticsearch
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private final ProductSearchService productSearchService;
    private final ProductFeignClient productFeignClient;

    /**
     * 监听商品同步队列
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_SYNC_QUEUE)
    public void handleProductSync(ProductSyncMessage message) {
        log.info("【MQ消费】收到商品同步消息，operationType={}, productId={}, productCode={}",
                message.getOperationType(), message.getProductId(), message.getProductCode());

        try {
            switch (message.getOperationType()) {
                case CREATE -> handleCreate(message);
                case UPDATE -> handleUpdate(message);
                case DELETE -> handleDelete(message);
                case ONLINE, OFFLINE -> handleUpdate(message);
                default -> log.warn("【MQ消费】未知操作类型，operationType={}", message.getOperationType());
            }
        } catch (Exception e) {
            log.error("【MQ消费】商品同步失败，operationType={}, productId={}",
                    message.getOperationType(), message.getProductId(), e);
            throw e; // 抛出异常触发 MQ 重试
        }
    }

    /**
     * 处理新增
     * 新增时消息包含完整数据，直接使用
     */
    private void handleCreate(ProductSyncMessage message) {
        ProductDocument document = convertToDocument(message);
        productSearchService.indexProduct(document);
        log.info("【MQ消费】商品新增同步成功，productId={}", message.getProductId());
    }

    /**
     * 处理修改/上下架
     * 通过 Feign 调用 product-service 查询数据库最新数据，保证数据一致性
     */
    private void handleUpdate(ProductSyncMessage message) {
        // 通过 Feign 查询数据库最新数据
        R<ProductDTO> result = productFeignClient.getById(message.getProductId());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            log.warn("【MQ消费】查询商品详情失败，productId={}, result={}", message.getProductId(), result);
            // 查询失败时，降级使用消息中的数据
            ProductDocument document = convertToDocument(message);
            productSearchService.indexProduct(document);
        } else {
            ProductDTO productDTO = result.getData();
            ProductDocument document = convertFromDTO(productDTO);
            // 保留消息中的状态（上下架操作可能改变状态）
            document.setStatus(message.getStatus());
            productSearchService.indexProduct(document);
        }
        log.info("【MQ消费】商品修改同步成功，productId={}", message.getProductId());
    }

    /**
     * 处理删除
     */
    private void handleDelete(ProductSyncMessage message) {
        productSearchService.deleteProduct(message.getProductId());
        log.info("【MQ消费】商品删除同步成功，productId={}", message.getProductId());
    }

    /**
     * 消息转换为 ES 文档
     */
    private ProductDocument convertToDocument(ProductSyncMessage message) {
        return ProductDocument.builder()
                .id(message.getProductId())
                .productCode(message.getProductCode())
                .productName(message.getProductName())
                .currentPrice(message.getCurrentPrice())
                .productType(convertCategoryToType(message.getProductType()))
                .exchangeCode(message.getExchangeCode())
                .status(message.getStatus())
                .updateTime(message.getOperateTime())
                .build();
    }

    /**
     * DTO 转换为 ES 文档
     */
    private ProductDocument convertFromDTO(ProductDTO dto) {
        return ProductDocument.builder()
                .id(dto.getId())
                .productCode(dto.getProductCode())
                .productName(dto.getProductName())
                .currentPrice(dto.getCurrentPrice())
                .productType(convertCategoryToType(dto.getCategory()))
                .status(dto.getStatus())
                .updateTime(dto.getUpdateTime())
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
}
