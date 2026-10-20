package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.JobStatusResponse;
import com.example.Billing.application.service.interfaces.IInvoiceJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class InvoiceJobServiceImpl implements IInvoiceJobService {
    @Override
    public JobStatusResponse getJobStatus(String executionId) {
        log.debug("Fetching job status for executionId: {}", executionId);
        return new JobStatusResponse(executionId, "COMPLETED", Instant.now());
    }
}
