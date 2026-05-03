package com.trade.ai.controller;

import com.trade.ai.dto.ChatRequest;
import com.trade.ai.dto.ChatResponse;
import com.trade.ai.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话接口
 * <p>
 * RESTful 接口，供前端或其他微服务调用 LLM 对话能力
 * 支持多轮对话（通过 sessionId 关联历史上下文）
 * </p>
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /ai/chat
     * 发送消息给 AI，返回回复（支持多轮对话）
     * <p>
     * 请求体示例：
     * {"message":"你好","sessionId":"xxx"}
     * 不传 sessionId 则自动新建会话
     * </p>
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.debug("收到对话请求: {}", request.message());
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /ai/chat/{sessionId}
     * 清空指定会话的历史消息
     */
    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<String> clearSession(@PathVariable String sessionId) {
        log.info("清空会话 sessionId={}", sessionId);
        chatService.clearSession(sessionId);
        return ResponseEntity.ok("会话已清空");
    }

    /**
     * GET /ai/models
     * 查询当前支持的模型列表
     */
    @GetMapping("/models")
    public ResponseEntity<Object> listModels() {
        return ResponseEntity.ok(new Object() {
            public final String defaultModel = "tencent/hy3-preview:free";
            public final String apiBase = "https://openrouter.ai/api/v1";
            public final String[] available = {
                    "tencent/hy3-preview:free",
                    "google/gemini-2.0-flash-exp:free",
                    "anthropic/claude-3-haiku:free",
                    "deepseek/deepseek-chat:free"
            };
        });
    }

    /**
     * GET /ai/health
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
