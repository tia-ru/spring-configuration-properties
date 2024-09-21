package org.springframework.configuration.maven.patch;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.rodnansol.core.generator.template.data.Property;
import org.rodnansol.core.generator.template.data.PropertyGroup;
import org.rodnansol.core.generator.template.data.SubTemplateData;

public class SubTemplateDataPatch extends SubTemplateData {
    public SubTemplateDataPatch(String moduleName, List<PropertyGroup> propertyGroups) {
        super(moduleName, propertyGroups);
    }

    @Override
    public List<Property> getAggregatedProperties() {
        return getPropertyGroups().stream()
                .flatMap(groups -> groups.getProperties().stream())
                .sorted( Comparator.comparing(Property::getFqName))
                .collect(Collectors.toList());
    }
}
