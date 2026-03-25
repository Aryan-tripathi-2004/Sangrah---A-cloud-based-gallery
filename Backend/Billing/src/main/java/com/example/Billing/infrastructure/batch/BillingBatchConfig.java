package com.example.Billing.infrastructure.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BillingBatchConfig {

    @Bean
    Job monthlyBillingJob(JobRepository jobRepository, Step monthlyBillingStep) {
        return new JobBuilder("monthlyBillingJob", jobRepository)
                .start(monthlyBillingStep)
                .build();
    }

    @Bean
    Step monthlyBillingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("monthlyBillingStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                .build();
    }
}
