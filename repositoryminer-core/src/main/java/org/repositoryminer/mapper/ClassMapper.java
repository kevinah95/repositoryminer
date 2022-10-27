package org.repositoryminer.mapper;

import metrics.examcompletemetric.MetricClass;
import metrics.examcompletemetric.MetricPackage;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.repositoryminer.domain.Class;
import org.repositoryminer.domain.Package;

import java.util.List;

@Mapper(uses = {MethodMapper.class})
public interface ClassMapper {
    ClassMapper INSTANCE = Mappers.getMapper(ClassMapper.class);
    @Mapping(source = "className", target = "name")
    @Mapping(source = "methods", target = "methods")
    Class convert(MetricClass metricClass);
    List<Class> convert(List<MetricClass> metricClassList);
}
