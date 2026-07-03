package com.trade.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档 DTO（跨服务传输对象）
 */
@Data
public class KnowledgeBaseDTO {

    private Long id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档原始内容
     */
    private String content;

    /**
     * 关联产品ID
     */
    private Long productId;

    /**
     * 文档类型：1=产品说明书 2=风险揭示书 3=常见问题 4=交易规则 5=其他
     */
    private Integer docType;

    /**
     * 状态：1=已发布 0=草稿 2=已下线
     */
    private Integer status;

    /**
     * 向量化状态：0=未向量化 1=已向量化 2=向量化失败
     */
    private Integer vectorStatus;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
