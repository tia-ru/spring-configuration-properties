package org.springframework.configuration.maven.patch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.rodnansol.core.generator.reader.MetadataConversionException;
import org.rodnansol.core.generator.template.data.Property;
import org.rodnansol.core.generator.template.data.PropertyDeprecation;
import org.rodnansol.core.generator.template.data.PropertyGroup;
import org.rodnansol.core.generator.template.data.PropertyGroupConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * Reads the spring-configuration-metadata.json file.
 *
 * @author nandorholozsnyak
 * @since 0.1.0
 */
public class MetadataReaderPatch {

    public static final MetadataReaderPatch INSTANCE = new MetadataReaderPatch();

    private static final Logger LOGGER = LoggerFactory.getLogger(org.rodnansol.core.generator.reader.MetadataReader.class);
    private static final String PACKAGE_JAVA_LANG = "java.lang.";
    private static final String PACKAGE_JAVA_UTIL = "java.util.";

    private MetadataReaderPatch() {
    }

    /**
     * Returns the properties in a map where the key is the name of the properties key and the values is the associated properties.
     *
     * @param metadataStream stream containing the content of the <code>spring-configuration-metadata.json</code>.
     * @return groups and properties converted to a Map.
     * @since 0.1.0
     */
    public Map<String, List<Property>> readPropertiesAsMap(InputStream metadataStream) {
        Objects.requireNonNull(metadataStream, "metadataStream is NULL");
        try {
            ConfigurationMetadata configurationMetadata = new JsonMarshaller().read(metadataStream);
            Map<String, List<Property>> propertyMap = getPropertyMap(configurationMetadata);
            LOGGER.trace("Configuration metadata contains number of properties:[{}]", propertyMap.size());
            return propertyMap;
        } catch (Exception e) {
            throw new MetadataConversionException("Error during converting properties to Map", e);
        }
    }

    /**
     * Returns a list of {@link PropertyGroup} instances from the given input stream.
     * <p>
     * <b>NOTE:</b> The current implementation is a bit fuzzy, when the time comes we can come up with a more efficient solution, but right now this is the "contact" basically.
     *
     * @param metadataStream stream containing the content of the <code>spring-configuration-metadata.json</code> or an empty list if the incoming stream is empty.
     * @return groups and properties converted to a List of {@link PropertyGroup}.
     * @since 0.1.0
     */
    public List<PropertyGroup> readPropertiesAsPropertyGroupList(InputStream metadataStream) {
        Objects.requireNonNull(metadataStream, "metadataStream is NULL");
        try {
            if (metadataStream.available() == 0) {
                return new ArrayList<>(); //must be modifiable
            }
            ConfigurationMetadata configurationMetadata = new JsonMarshaller().read(metadataStream);
            Map<String, List<Property>> propertyMap = getPropertyMap(configurationMetadata);
            Map<String, List<PropertyGroup>> propertyGroupsByType = getPropertyGroups(configurationMetadata);
            updateGroupsWithPropertiesAndAssociations(propertyMap, propertyGroupsByType);
            LOGGER.trace("Configuration metadata contains number of group:[{}] and properties:[{}]", propertyGroupsByType.size(), propertyMap.size());
            return flattenValues(propertyGroupsByType);
        } catch (Exception e) {
            throw new MetadataConversionException("Error during converting properties to list of ProperyGroups", e);
        }
    }

    private Property updateProperty(PropertyGroup propertyGroup, Property property) {
        String groupName = propertyGroup.getGroupName();
        if (propertyGroup.isUnknownGroup()) {
            property.setKey(property.getFqName());
        } else {
            if(propertyGroup.getGroupName().isBlank()) {
                property.setKey(property.getFqName());
            } else {
                property.setKey(property.getFqName().substring(groupName.length() + 1));
            }
        }
        return property;
    }

    private List<PropertyGroup> flattenValues(Map<String, List<PropertyGroup>> propertyGroupsByType) {
        return propertyGroupsByType.values()
                .stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(propertyGroup -> propertyGroup.getSourceType().toLowerCase()))
                .collect(Collectors.toList());
    }

    private void updateGroupsWithPropertiesAndAssociations(Map<String, List<Property>> propertyMap, Map<String, List<PropertyGroup>> propertyGroupsByType) {
        for (Map.Entry<String, List<PropertyGroup>> entry : propertyGroupsByType.entrySet()) {
            List<PropertyGroup> nestedProperties = updatePropertiesAndReturnNestedGroups(propertyMap, entry);
            for (PropertyGroup nestedProperty : nestedProperties) {
                List<PropertyGroup> parentList = propertyGroupsByType.get(nestedProperty.getSourceType());
                parentList.stream().filter(propertyGroup -> propertyGroup.getType().equals(nestedProperty.getSourceType())).findFirst().ifPresent(parent -> {
                    parent.addChildGroup(nestedProperty);
                    nestedProperty.setParentGroup(parent);
                });
            }
        }
    }

    private List<PropertyGroup> updatePropertiesAndReturnNestedGroups(Map<String, List<Property>> propertyMap, Map.Entry<String, List<PropertyGroup>> propertyEntry) {
        return propertyEntry.getValue()
                .stream()
                .map(propertyGroup -> setProperties(propertyMap, propertyGroup))
                .filter(PropertyGroup::isNested)
                .collect(Collectors.toList());
    }

    private PropertyGroup setProperties(Map<String, List<Property>> propertyMap, PropertyGroup propertyGroup) {
        List<Property> properties = propertyMap.get(propertyGroup.getType());
        if ((properties == null || properties.isEmpty())) {
            if (!propertyGroup.isUnknownGroup()) {
                LOGGER.warn(
                        "Property group with name:[{}] is having no properties, please check if you provided the getter/setter methods. If your class is empty intentionally, please forget this warning here.",
                        propertyGroup.getGroupName());
            }
            return propertyGroup;
        }
        List<Property> collectedProperties = properties.stream()
                .filter(property -> property.getFqName().startsWith(propertyGroup.getGroupName()) || propertyGroup.isUnknownGroup())
                .map(property -> updateProperty(propertyGroup, property))
                .collect(Collectors.toList());
        propertyGroup.setProperties(collectedProperties);
        return propertyGroup;
    }

    private Map<String, List<PropertyGroup>> getPropertyGroups(ConfigurationMetadata configurationMetadata) {
        Map<String, List<PropertyGroup>> propertyGroupMap = configurationMetadata.getItems()
                .stream()
                .filter(itemMetadata -> itemMetadata.isOfItemType(ItemMetadata.ItemType.GROUP))
                .map(itemMetadata -> new PropertyGroup(itemMetadata.getName(), itemMetadata.getType(), getSourceTypeOrDefault(itemMetadata)))
                .collect(Collectors.groupingBy(PropertyGroup::getSourceType, Collectors.toList()));
        List<PropertyGroup> value = new ArrayList<>();
        value.add(PropertyGroup.createUnknownGroup());
        propertyGroupMap.put(PropertyGroupConstants.UNKNOWN, value);
        return propertyGroupMap;
    }

    private Map<String, List<Property>> getPropertyMap(ConfigurationMetadata configurationMetadata) {
        Function<ItemMetadata, String> getSourceType = this::getSourceTypeOrDefault;
        return configurationMetadata.getItems()
                .stream()
                .filter(itemMetadata -> itemMetadata.isOfItemType(ItemMetadata.ItemType.PROPERTY))
                .collect(Collectors.groupingBy(getSourceType,
                        Collectors.mapping(this::mapToProperty, Collectors.toList()))
                );
    }

    private String getSourceTypeOrDefault(ItemMetadata current) {
        return Optional.ofNullable(current.getSourceType()).orElse(PropertyGroupConstants.UNKNOWN);
    }

    private Property mapToProperty(ItemMetadata itemMetadata) {

        String type = simplifyType(itemMetadata.getType());
        Property property = new Property(itemMetadata.getName(), type);
        property.setDescription(itemMetadata.getDescription());
        if (itemMetadata.getDefaultValue() != null) {
            property.setDefaultValue(itemMetadata.getDefaultValue().toString());
        }
        ItemDeprecation deprecation = itemMetadata.getDeprecation();
        if (deprecation != null) {
            /*String reason = deprecation.getReason() == null ? "unknown" : deprecation.getReason();
            String replacement = deprecation.getReplacement() == null ? "unknown" : deprecation.getReplacement();*/
            PropertyDeprecation propertyDeprecation = new PropertyDeprecationPatch(deprecation.getReason(), deprecation.getReplacement());
            property.setPropertyDeprecation(propertyDeprecation);
        }
        return property;
    }

    private String simplifyType(String type) {
        if (type == null) {
            type = "";
        }
        type = type.replace(PACKAGE_JAVA_LANG,"");
        type = type.replace(PACKAGE_JAVA_UTIL,"");

        return type;
    }
}
