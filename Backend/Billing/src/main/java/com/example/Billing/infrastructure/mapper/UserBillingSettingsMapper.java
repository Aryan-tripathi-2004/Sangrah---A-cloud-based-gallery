package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserBillingSettingsMapper {

    public UserBillingSettingsDTO toDTO(UserBillingSettingsDocument document) {
        if (document == null) {
            return null;
        }

        Boolean autoPay = document.getPreferences() != null ? document.getPreferences().getAutoPayEnabled() : null;
        Boolean invoiceEmail = document.getPreferences() != null ? document.getPreferences().getInvoiceEmail() : null;
        BigDecimal threshold = document.getPreferences() != null ? document.getPreferences().getCostAlertThreshold() : BigDecimal.ZERO;

        return UserBillingSettingsDTO.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .stripeCustomerId(document.getStripeCustomerId())
                .totalPaidAllTime(document.getTotalPaidAllTime())
                .outstandingBalance(document.getOutstandingBalance())
                .autoPayEnabled(autoPay)
                .invoiceEmail(invoiceEmail)
                .costAlertThreshold(threshold)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
