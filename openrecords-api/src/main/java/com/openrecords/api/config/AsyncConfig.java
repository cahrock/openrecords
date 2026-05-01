package com.openrecords.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration.
 *
 * Email sending uses a dedicated thread pool so SMTP latency doesn't
 * block request-handling threads. Thread pool size is generous for
 * the expected scale of a portfolio app.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for asynchronous email sending.
     *
     * Pool sizing:
     * - corePoolSize 2: minimum threads kept alive
     * - maxPoolSize 5: scales up under load (still trivial vs SMTP latency)
     * - queueCapacity 100: pending tasks before back-pressure kicks in
     *
     * If the queue fills (100+ pending), the calling thread blocks until
     * a slot opens. Better than dropping emails silently.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}