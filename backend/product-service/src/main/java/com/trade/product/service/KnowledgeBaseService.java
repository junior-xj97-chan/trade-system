package com.trade.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.trade.product.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库文档 Service
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 根据文档类型查询已发布文档
     */
    List<KnowledgeBase> listByDocType(Integer docType);

    /**
     * 根据产品ID查询关联文档
     */
    List<KnowledgeBase> listByProductId(Long productId);

    /**
     * 查询未向量化的文档
     */
    List<KnowledgeBase> listUnvectorized();

    /**
     * 更新向量化状态
     */
    boolean updateVectorStatus(Long id, Integer vectorStatus);
}
