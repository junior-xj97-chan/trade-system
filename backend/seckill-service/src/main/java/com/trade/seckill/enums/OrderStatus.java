package com.trade.seckill.enums;

public enum OrderStatus {
    PENDING_PAY(1, "待支付"),
    PAID(2, "已支付"),
    TIMEOUT_CANCELLED(3, "超时作废");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
