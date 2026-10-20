package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.JobStatusResponse;

public interface IInvoiceJobService {
    JobStatusResponse getJobStatus(String executionId);
}
