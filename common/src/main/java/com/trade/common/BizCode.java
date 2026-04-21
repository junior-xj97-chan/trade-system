package com.trade.common;

/**
 * 业务错误码枚举
 */
public enum BizCode {
    // 通用错误 1xxx
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(1000, "系统繁忙，请稍后重试"),
    PARAM_ERROR(1001, "参数错误"),
    NULL_POINT_ERROR(1002, "空指针异常"),
    BUSINESS_ERROR(1003, "业务处理异常"),

    // 用户模块 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_EXIST(2002, "用户已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    TOKEN_EXPIRED(2004, "token已过期"),
    TOKEN_INVALID(2005, "token无效"),

    // 账户模块 3xxx
    ACCOUNT_NOT_FOUND(3001, "账户不存在"),
    BALANCE_NOT_ENOUGH(3002, "余额不足"),
    ACCOUNT_FROZEN(3003, "账户已冻结"),

    // 订单模块 4xxx
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态异常"),
    ORDER_CANCELLED(4003, "订单已取消"),
    ORDER_COMPLETED(4004, "订单已完成"),

    // 交易模块 5xxx
    TRADE_NOT_FOUND(5001, "交易不存在"),
    TRADE_PRICE_ERROR(5002, "价格异常"),
    TRADE_AMOUNT_ERROR(5003, "交易数量异常"),
    MARKET_CLOSED(5004, "市场已关闭"),

    // 持仓模块 6xxx
    POSITION_NOT_FOUND(6001, "持仓不存在"),
    POSITION_NOT_ENOUGH(6002, "持仓数量不足"),

    // 限流模块 7xxx
    RATE_LIMIT_EXCEEDED(7001, "请求过于频繁，请稍后重试");

    private final Integer code;
    private final String message;

    BizCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
