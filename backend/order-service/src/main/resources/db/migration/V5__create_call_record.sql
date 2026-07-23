-- =============================================================
-- V5: 创建接口调用流水表，用于跨服务调用落库与补偿重试
-- =============================================================

CREATE TABLE IF NOT EXISTS t_call_record (
    id            BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    biz_no        VARCHAR(64)   NOT NULL COMMENT '业务流水号（如订单号）',
    biz_type      VARCHAR(32)   NOT NULL COMMENT '业务类型（PAY:支付 SELL:卖出 CANCEL:取消）',
    target_service VARCHAR(64)  NOT NULL COMMENT '下游服务名',
    target_method VARCHAR(128)  NOT NULL COMMENT '下游方法/接口标识',
    request_param TEXT          NULL     COMMENT '请求参数 JSON',
    response      TEXT          NULL     COMMENT '响应结果 JSON',
    status        INT           DEFAULT 0 COMMENT '0:初始化 1:处理中 2:成功 3:失败 4:人工处理',
    retry_count   INT           DEFAULT 0 COMMENT '重试次数',
    max_retry     INT           DEFAULT 5 COMMENT '最大重试次数',
    error_msg     VARCHAR(1024) NULL     COMMENT '失败原因',
    create_time   DATETIME      NULL     COMMENT '创建时间',
    update_time   DATETIME      NULL     COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_target (biz_no, target_service, target_method),
    KEY idx_status_retry (status, retry_count),
    KEY idx_biz_no (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口调用流水表';
