package com.trade.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_call_record")
public class CallRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bizNo;         // 业务流水号（订单号）
    private String bizType;       // 业务类型 PAY/SELL/CANCEL
    private String targetService; // 下游服务名
    private String targetMethod;  // 下游方法标识
    private String requestParam;  // 请求参数 JSON
    private String response;      // 响应结果 JSON
    private Integer status;       // 0:初始化 1:处理中 2:成功 3:失败 4:人工处理
    private Integer retryCount;   // 重试次数
    private Integer maxRetry;     // 最大重试次数
    private String errorMsg;      // 失败原因

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
