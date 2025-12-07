package de.softwaretesting.studyconnect.mappers.request;

import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserRequestMapper extends EntityMapper<User, UserUpdateRequestDTO> {

  @org.mapstruct.Mapping(target = "id", ignore = true)
  @org.mapstruct.Mapping(target = "keycloakUUID", ignore = true)
  @org.mapstruct.Mapping(target = "createdAt", ignore = true)
  User toEntity(UserUpdateRequestDTO dto);

  UserResponseDTO toDto(User entity);
}
