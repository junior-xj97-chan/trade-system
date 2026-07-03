-- =============================================================
-- V3: 创建知识库文档表
-- 用于 RAG 问答系统的金融产品知识库
-- =============================================================

-- 知识库文档表
CREATE TABLE IF NOT EXISTS t_knowledge_base (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(200)  NOT NULL COMMENT '文档标题',
    content       LONGTEXT      NOT NULL COMMENT '文档原始内容（Markdown/纯文本）',
    product_id    BIGINT       DEFAULT NULL COMMENT '关联产品ID（可选）',
    doc_type      INT          NOT NULL DEFAULT 1 COMMENT '文档类型：1=产品说明书 2=风险揭示书 3=常见问题 4=交易规则 5=其他',
    status        INT          NOT NULL DEFAULT 1 COMMENT '状态：1=已发布 0=草稿 2=已下线',
    vector_status INT          NOT NULL DEFAULT 0 COMMENT '向量化状态：0=未向量化 1=已向量化 2=向量化失败',
    version       INT          DEFAULT 0 COMMENT '乐观锁版本号',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       INT          DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id),
    KEY idx_doc_type (doc_type),
    KEY idx_status (status),
    KEY idx_vector_status (vector_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';
