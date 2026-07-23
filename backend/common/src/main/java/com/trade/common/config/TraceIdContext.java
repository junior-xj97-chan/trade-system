package com.trade.common.config;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceIdContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceIdContext() {}

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}
