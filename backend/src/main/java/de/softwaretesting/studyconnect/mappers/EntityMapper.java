package de.softwaretesting.studyconnect.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityMapper<D, E> {
  D toDto(E entity);

  E toEntity(D dto);

  @Named("partialUpdate")
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void partialUpdate(@MappingTarget E entity, D dto);
}
