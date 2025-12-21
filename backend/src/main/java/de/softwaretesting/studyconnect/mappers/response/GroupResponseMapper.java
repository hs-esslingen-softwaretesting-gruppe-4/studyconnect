package de.softwaretesting.studyconnect.mappers.response;

import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Group;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupResponseMapper extends EntityMapper<GroupResponseDTO, Group> {

  @Mapping(target = "isPublic", source = "public")
  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "lastUpdatedAt", source = "updatedAt")
  @Override
  GroupResponseDTO toDto(Group entity);

  @Mapping(target = "isPublic", source = "public")
  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "lastUpdatedAt", source = "updatedAt")
  List<GroupResponseDTO> toDtoList(List<Group> entities);
}
