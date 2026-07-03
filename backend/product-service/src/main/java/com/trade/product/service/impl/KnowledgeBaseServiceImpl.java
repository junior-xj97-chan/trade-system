package com.trade.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trade.product.entity.KnowledgeBase;
import com.trade.product.mapper.KnowledgeBaseMapper;
import com.trade.product.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库文档 Service 实现类
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    @Override
    public List<KnowledgeBase> listByDocType(Integer docType) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getDocType, docType)
                .eq(KnowledgeBase::getStatus, 1)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByDesc(KnowledgeBase::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<KnowledgeBase> listByProductId(Long productId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getProductId, productId)
                .eq(KnowledgeBase::getStatus, 1)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByDesc(KnowledgeBase::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<KnowledgeBase> listUnvectorized() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getVectorStatus, 0)
                .eq(KnowledgeBase::getStatus, 1)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByAsc(KnowledgeBase::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public boolean updateVectorStatus(Long id, Integer vectorStatus) {
        KnowledgeBase entity = new KnowledgeBase();
        entity.setId(id);
        entity.setVectorStatus(vectorStatus);
        return this.updateById(entity);
    }
}
