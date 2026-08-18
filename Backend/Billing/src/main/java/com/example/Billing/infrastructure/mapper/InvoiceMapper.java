package com.example.Billing.infrastructure.mapper;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InvoiceMapper {
    InvoiceDTO toDTO(InvoiceDocument document);
}
