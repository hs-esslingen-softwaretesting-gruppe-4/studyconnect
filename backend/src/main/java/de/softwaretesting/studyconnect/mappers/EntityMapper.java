package de.softwaretesting.studyconnect.mappers;

import java.util.List;

import org.mapstruct.*;

@MapperConfig(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityMapper<DTOType, EntityType> {
    DTOType toDto(EntityType entity);
    EntityType toEntity(DTOType dto);

    List<DTOType> toDto(List<EntityType> entityList);
    List<EntityType> toEntity(List<DTOType> dtoList);

    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(@MappingTarget EntityType entity, DTOType dto);

}
