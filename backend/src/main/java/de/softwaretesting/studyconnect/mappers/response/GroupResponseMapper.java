package de.softwaretesting.studyconnect.mappers.response;

import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupResponseMapper extends EntityMapper<GroupResponseDTO, Group> {

  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "lastUpdatedAt", source = "updatedAt")
  @Mapping(target = "adminIds", source = "admins", qualifiedByName = "adminIdsMapping")
  @Mapping(target = "isPrivate", expression = "java(!entity.isPublic())")
  @Override
  GroupResponseDTO toDto(Group entity);

  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "lastUpdatedAt", source = "updatedAt")
  @Mapping(target = "adminIds", source = "admins", qualifiedByName = "adminIdsMapping")
  @Mapping(target = "isPrivate", expression = "java(!entity.isPublic())")
  List<GroupResponseDTO> toDtoList(List<Group> entities);

  @Named("adminIdsMapping")
  default Set<Long> mapAdminsToIds(Set<User> admins) {
    if (admins == null) {
      return Set.of();
    }
    return admins.stream().map(User::getId).collect(Collectors.toSet());
  }
}
