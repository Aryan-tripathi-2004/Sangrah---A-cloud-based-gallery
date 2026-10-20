package com.example.Billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool configuration for asynchronous background event processing.
 * Offloads heavy PDF generation and remote Feign client calls away from Tomcat HTTP worker threads
 * to prevent webhook connection timeouts (e.g., Stripe webhooks).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = {"billingTaskExecutor", "taskExecutor"})
    public Executor billingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("billing-async-");
        executor.initialize();
        return executor;
    }
}
