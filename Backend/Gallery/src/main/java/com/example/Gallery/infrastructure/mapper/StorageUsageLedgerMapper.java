package com.example.Gallery.infrastructure.mapper;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface StorageUsageLedgerMapper {
    StorageUsageLedgerDTO toDTO(StorageUsageLedgerDocument document);
}
