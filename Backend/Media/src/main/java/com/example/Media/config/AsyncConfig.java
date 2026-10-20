package com.example.Media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool configuration for asynchronous media background tasks (e.g. bulk storage purge).
 * Prevents HTTP worker thread starvation and connection pool exhaustion by offloading
 * N+1 physical file I/O operations from request processing threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${media.async.core-pool-size}")
    private int corePoolSize;

    @Value("${media.async.max-pool-size}")
    private int maxPoolSize;

    @Value("${media.async.queue-capacity}")
    private int queueCapacity;

    @Value("${media.async.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean(name = {"mediaTaskExecutor", "taskExecutor"})
    public Executor mediaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
