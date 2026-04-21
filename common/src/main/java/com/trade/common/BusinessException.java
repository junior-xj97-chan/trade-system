package com.trade.common;

/**
 * 业务异常
 * <p>
 * 用于封装带错误码的业务错误，便于前端精准处理
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
