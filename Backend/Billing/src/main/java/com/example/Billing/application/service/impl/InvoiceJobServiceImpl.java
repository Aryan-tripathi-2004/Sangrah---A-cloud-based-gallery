package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.JobStatusResponse;
import com.example.Billing.application.service.interfaces.IInvoiceJobService;
import com.example.Billing.shared.exception.ForbiddenOperationException;
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

    @Override
    public JobStatusResponse getAuthorizedJobStatus(String executionId, String role) {
        if (role == null || (!role.equalsIgnoreCase("ROLE_ADMIN") && !role.equalsIgnoreCase("ADMIN"))) {
            throw new ForbiddenOperationException(
                    "Administrative privileges required to access billing job operations");
        }
        return getJobStatus(executionId);
    }
}
