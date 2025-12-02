package de.softwaretesting.studyconnect.mappers.request;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import de.softwaretesting.studyconnect.dtos.request.UserRequestDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserRequestMapper extends EntityMapper<UserRequestDTO, User> {

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "keycloakUUID", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRequestDTO dto);
}
