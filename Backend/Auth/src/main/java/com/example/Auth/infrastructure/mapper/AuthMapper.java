package com.example.Auth.infrastructure.mapper;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.infrastructure.persistence.document.UserDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AuthMapper {
    UserDTO toUserDTO(UserDocument userDocument);
}
