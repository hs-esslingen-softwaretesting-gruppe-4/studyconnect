package de.softwaretesting.studyconnect.mappers.response;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;
import de.softwaretesting.studyconnect.mappers.EntityMapper;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskResponseMapper extends EntityMapper<TaskResponseDTO, Task> {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "assigneeIds", source = "assignees", qualifiedByName = "assigneeIdsMapping")
    @Mapping(target = "groupId", source = "group.id")
    TaskResponseDTO toDto(Task entity);

    @Named("assigneeIdsMapping")
    default Set<Long> mapAssigneesToIds(Set<User> assignees) {
        return assignees.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

}
