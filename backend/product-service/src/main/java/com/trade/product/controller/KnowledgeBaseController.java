package com.trade.product.controller;

import com.trade.common.R;
import com.trade.common.entity.KnowledgeBaseDTO;
import com.trade.product.entity.KnowledgeBase;
import com.trade.product.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/product/knowledge")
@RequiredArgsConstructor
@Validated
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBaseDTO toDTO(KnowledgeBase entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private List<KnowledgeBaseDTO> toDTOList(List<KnowledgeBase> entities) {
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 新增知识库文档
     */
    @PostMapping
    public R<KnowledgeBaseDTO> create(@RequestBody @Validated KnowledgeBase knowledgeBase) {
        knowledgeBase.setVectorStatus(0);
        knowledgeBaseService.save(knowledgeBase);
        return R.ok(toDTO(knowledgeBase));
    }

    /**
     * 更新知识库文档
     */
    @PutMapping("/{id}")
    public R<Void> update(
            @PathVariable Long id,
            @RequestBody @Validated KnowledgeBase knowledgeBase) {
        knowledgeBase.setId(id);
        knowledgeBase.setVectorStatus(0); // 更新后标记为未向量化
        knowledgeBaseService.updateById(knowledgeBase);
        return R.ok();
    }

    /**
     * 删除知识库文档（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.removeById(id);
        return R.ok();
    }

    /**
     * 根据ID查询文档
     */
    @GetMapping("/{id}")
    public R<KnowledgeBaseDTO> getById(@PathVariable Long id) {
        KnowledgeBase entity = knowledgeBaseService.getById(id);
        return entity != null ? R.ok(toDTO(entity)) : R.fail(404, "文档不存在");
    }

    /**
     * 查询所有已发布文档
     */
    @GetMapping
    public R<List<KnowledgeBaseDTO>> listAll() {
        List<KnowledgeBase> list = knowledgeBaseService.list();
        return R.ok(toDTOList(list));
    }

    /**
     * 根据文档类型查询
     */
    @GetMapping("/type/{docType}")
    public R<List<KnowledgeBaseDTO>> listByDocType(@PathVariable Integer docType) {
        List<KnowledgeBase> list = knowledgeBaseService.listByDocType(docType);
        return R.ok(toDTOList(list));
    }

    /**
     * 根据产品ID查询关联文档
     */
    @GetMapping("/product/{productId}")
    public R<List<KnowledgeBaseDTO>> listByProductId(@PathVariable Long productId) {
        List<KnowledgeBase> list = knowledgeBaseService.listByProductId(productId);
        return R.ok(toDTOList(list));
    }

    /**
     * 查询未向量化的文档（供 ai-service 调用）
     */
    @GetMapping("/unvectorized")
    public R<List<KnowledgeBaseDTO>> listUnvectorized() {
        List<KnowledgeBase> list = knowledgeBaseService.listUnvectorized();
        return R.ok(toDTOList(list));
    }

    /**
     * 更新向量化状态（供 ai-service 回调，使用 POST 避免 RestTemplate PATCH 兼容性问题）
     */
    @PostMapping("/{id}/vector-status")
    public R<Void> updateVectorStatus(
            @PathVariable Long id,
            @RequestParam Integer vectorStatus) {
        boolean success = knowledgeBaseService.updateVectorStatus(id, vectorStatus);
        return success ? R.ok() : R.fail("更新失败");
    }
}
