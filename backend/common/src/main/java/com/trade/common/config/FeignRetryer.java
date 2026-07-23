package com.trade.common.config;

import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignRetryer implements Retryer {

    private final int maxAttempts;
    private final long backoff;
    private int attempt;

    public FeignRetryer() {
        this(2, 100);
    }

    public FeignRetryer(int maxAttempts, long backoff) {
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
        this.attempt = 1;
    }

    @Override
    public void continueOrPropagate(RetryableException e) {
        if (e.request().httpMethod() == null ||
            !e.request().httpMethod().name().equalsIgnoreCase("GET")) {
            throw e;
        }

        if (attempt++ >= maxAttempts) {
            throw e;
        }

        try {
            log.warn("Feign GET请求重试，第{}次，url={}", attempt, e.request().url());
            Thread.sleep(backoff);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public Retryer clone() {
        return new FeignRetryer(maxAttempts, backoff);
    }
}
