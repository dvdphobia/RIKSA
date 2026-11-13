package com.riksa.auth.email;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/** Drops rejected email tasks without blocking request threads or exposing task contents. */
@Slf4j
public class ResendEmailRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        log.warn("Verification email task was rejected by the bounded delivery executor");
    }
}
