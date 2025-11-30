package de.softwaretesting.studyconnect.mappers.request;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Task;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskRequestMapper extends EntityMapper<TaskRequestDTO, Task> {

    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "assignees", ignore = true)
    @org.mapstruct.Mapping(target = "group", ignore = true)
    Task toEntity(TaskRequestDTO dto);

}
