package com.riksa.auth.email;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ResendEmailRejectedExecutionHandlerTest {

    @Test
    void saturatedExecutorDropsTaskWithoutBlockingOrThrowing() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ResendEmailRejectedExecutionHandler());
        executor.initialize();

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean queuedTaskRan = new AtomicBoolean();
        AtomicBoolean rejectedTaskRan = new AtomicBoolean();

        try {
            executor.execute(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

            executor.execute(() -> queuedTaskRan.set(true));
            assertThatCode(() -> executor.execute(() -> rejectedTaskRan.set(true)))
                    .doesNotThrowAnyException();
            assertThat(rejectedTaskRan).isFalse();
        } finally {
            release.countDown();
            executor.shutdown();
            executor.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS);
        }

        assertThat(queuedTaskRan).isTrue();
    }
}
