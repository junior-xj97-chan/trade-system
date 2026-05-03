package com.trade.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 对话请求
 */
public record ChatRequest(
        @NotBlank(message = "消息内容不能为空")
        String message,

        // 可选：对话历史 ID（用于上下文关联）
        String sessionId,

        // 可选：模型覆盖
        String model
) {
}
