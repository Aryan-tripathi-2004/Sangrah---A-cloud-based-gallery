package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    imports = {BigDecimal.class}
)
public interface UserBillingSettingsMapper {

    @Mapping(target = "autoPayEnabled", source = "preferences.autoPayEnabled")
    @Mapping(target = "invoiceEmail", source = "preferences.invoiceEmail")
    @Mapping(
        target = "costAlertThreshold",
        expression = "java(document.getPreferences() != null && document.getPreferences().getCostAlertThreshold() != null ? document.getPreferences().getCostAlertThreshold() : BigDecimal.ZERO)"
    )
    UserBillingSettingsDTO toDTO(UserBillingSettingsDocument document);
}
