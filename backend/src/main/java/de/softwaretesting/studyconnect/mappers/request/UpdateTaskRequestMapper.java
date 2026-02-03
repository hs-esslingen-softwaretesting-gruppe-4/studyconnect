package de.softwaretesting.studyconnect.mappers.request;

import de.softwaretesting.studyconnect.dtos.request.UpdateTaskRequestDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Task;
import org.mapstruct.Mapper;

@Mapper(
    unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
    componentModel = org.mapstruct.MappingConstants.ComponentModel.SPRING)
public interface UpdateTaskRequestMapper extends EntityMapper<UpdateTaskRequestDTO, Task> {

  @org.mapstruct.Mapping(target = "createdById", ignore = true)
  @org.mapstruct.Mapping(target = "assigneeIds", ignore = true)
  @org.mapstruct.Mapping(target = "title", ignore = true)
  UpdateTaskRequestDTO toDto(Task entity);

  @org.mapstruct.Mapping(source = "createdById", target = "createdBy", ignore = true)
  @org.mapstruct.Mapping(source = "assigneeIds", target = "assignees", ignore = true)
  @org.mapstruct.Mapping(target = "group", ignore = true)
  Task toEntity(UpdateTaskRequestDTO dto);
}
