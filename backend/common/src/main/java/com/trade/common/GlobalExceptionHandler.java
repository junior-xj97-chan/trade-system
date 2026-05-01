package com.trade.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.exception.TransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一处理 Seata 分布式事务异常和业务异常，返回友好错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== Seata 事务异常 ====================

    /**
     * Seata 分布式事务全局回滚异常
     * 触发场景：任一分支事务失败，全局回滚
     */
    @ExceptionHandler(TransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleTransactionException(TransactionException e) {
        log.error("【Seata事务异常】分布式事务执行失败，触发全局回滚", e);
        return R.fail(BizCode.BUSINESS_ERROR);
    }

    // ==================== 业务异常 ====================

    /**
     * 业务异常（BusinessException）
     * 触发场景：余额不足、账户不存在、订单状态异常等
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("【业务异常】code={}，msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 通用运行时异常
     * 触发场景：参数错误、空指针、Nacos/Feign 调用失败等
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleRuntimeException(RuntimeException e) {
        log.error("【运行时异常】{}", e.getMessage(), e);
        return R.fail(e.getMessage());
    }

    /**
     * 通用异常兜底
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("【系统异常】{}", e.getMessage(), e);
        return R.fail(BizCode.SYSTEM_ERROR);
    }
}
