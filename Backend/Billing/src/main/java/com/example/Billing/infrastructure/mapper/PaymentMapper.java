package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.infrastructure.persistence.document.PaymentDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PaymentMapper {
    PaymentDTO toDTO(PaymentDocument document);
}
