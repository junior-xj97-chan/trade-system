package com.trade.ai.dto;

/**
 * AI 对话响应
 */
public record ChatResponse(
        // AI 返回的文本内容
        String content,

        // 使用的模型
        String model,

        // 消耗的 Token 数量（估算）
        Long usageTokens,

        // 本次对话 ID
        String sessionId
) {
}
