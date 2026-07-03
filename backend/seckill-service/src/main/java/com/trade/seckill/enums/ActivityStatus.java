package com.trade.seckill.enums;

public enum ActivityStatus {
    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final int code;
    private final String desc;

    ActivityStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static ActivityStatus fromCode(int code) {
        for (ActivityStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知活动状态: " + code);
    }
}
