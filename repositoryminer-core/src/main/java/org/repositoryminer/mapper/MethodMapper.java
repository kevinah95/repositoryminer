package org.repositoryminer.mapper;

import metrics.examcompletemetric.MetricClass;
import metrics.examcompletemetric.MetricMethod;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.repositoryminer.domain.Class;
import org.repositoryminer.domain.Method;

import java.util.List;

@Mapper
public interface MethodMapper {
    MethodMapper INSTANCE = Mappers.getMapper(MethodMapper.class);
    default Method convert(MetricMethod metricMethod){
        return new Method(metricMethod.getMethodName(), metricMethod.getLOC(), metricMethod.getCYCLO());
    };

    List<Method> convert(List<MetricMethod> metricMethodList);
}
