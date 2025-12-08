package de.softwaretesting.studyconnect.mappers.request;

import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskRequestMapper extends EntityMapper<Task, TaskRequestDTO> {

  @org.mapstruct.Mapping(target = "createdById", ignore = true)
  @org.mapstruct.Mapping(target = "assigneeIds", ignore = true)
  @org.mapstruct.Mapping(target = "title", ignore = true)
  TaskRequestDTO toDto(Task entity);

  @org.mapstruct.Mapping(source = "createdById", target = "createdBy", ignore = true)
  @org.mapstruct.Mapping(source = "assigneeIds", target = "assignees", ignore = true)
  @org.mapstruct.Mapping(target = "group", ignore = true)
  Task toEntity(TaskRequestDTO dto);
}
