package de.softwaretesting.studyconnect.mappers.response;

import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserResponseMapper extends EntityMapper<UserResponseDTO, User> {
  @Override
  UserResponseDTO toDto(User entity);

  List<UserResponseDTO> toDtoList(List<User> entities);
}
