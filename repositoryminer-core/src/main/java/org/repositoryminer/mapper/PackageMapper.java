package org.repositoryminer.mapper;

import metrics.examcompletemetric.MetricPackage;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.repositoryminer.domain.Package;

import java.util.ArrayList;
import java.util.List;

@Mapper(uses = { ClassMapper.class })
public interface PackageMapper {
    PackageMapper INSTANCE = Mappers.getMapper(PackageMapper.class);
    @Mapping(source = "packageName", target = "name")
    @Mapping(source = "packageClasses", target = "classes")
    Package convert(MetricPackage metricPackage);
    List<Package> convert(List<MetricPackage> metricPackageList);
}
