package org.springframework.configuration.maven.patch;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.rodnansol.core.generator.template.data.MainTemplateData;
import org.rodnansol.core.generator.template.data.Property;
import org.rodnansol.core.generator.template.data.PropertyGroup;

public class MainTemplateDataPatch extends MainTemplateData {
    public MainTemplateDataPatch(String mainName, List<PropertyGroup> propertyGroups) {
        super(mainName, propertyGroups);
    }

    @Override
    public List<Property> getAggregatedProperties() {
        return getPropertyGroups().stream()
                .flatMap(groups -> groups.getProperties().stream())
                .sorted( Comparator.comparing(Property::getFqName))
                .distinct()
                .collect(Collectors.toList());
    }
}
