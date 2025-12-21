package de.softwaretesting.studyconnect.mappers.request;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Group;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface CreateGroupRequestMapper extends EntityMapper<CreateGroupRequestDTO, Group> {

  @org.mapstruct.Mapping(target = "createdById", ignore = true)
  @org.mapstruct.Mapping(target = "adminIds", ignore = true)
  @org.mapstruct.Mapping(target = "memberIds", ignore = true)
  CreateGroupRequestDTO toDto(Group entity);

  @org.mapstruct.Mapping(target = "id", ignore = true)
  @org.mapstruct.Mapping(target = "createdAt", ignore = true)
  @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
  @org.mapstruct.Mapping(target = "inviteCode", ignore = true)
  @org.mapstruct.Mapping(target = "memberCount", ignore = true)
  @org.mapstruct.Mapping(target = "createdBy", ignore = true)
  @org.mapstruct.Mapping(target = "members", ignore = true)
  @org.mapstruct.Mapping(target = "admins", ignore = true)
  @org.mapstruct.Mapping(target = "tasks", ignore = true)
  @org.mapstruct.Mapping(target = "public", source = "isPublic")
  Group toEntity(CreateGroupRequestDTO dto);
}
