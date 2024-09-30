# Spring Configuration Property Documenter
![Spring Configuration Property Documenter at Maven Central](https://img.shields.io/maven-central/v/io.github.tia-ru/spring-configuration-properties?style=plastic&logo=apachemaven&logoColor=%23C71A36)
![GitHub License](https://img.shields.io/github/license/tia-ru/spring-configuration-properties?style=plastic) 

This project provides a set of tools for generating a document that lists the configuration properties of
a Spring Framework-based project from xml config files and java annotations.

-------
![](src/site/resources/images/markdown-result.png "Result example")
-------
The tools are like [spring-boot-configuration-processor](https://docs.spring.io/spring-boot/specification/configuration-metadata/annotation-processor.html)
and [Spring Configuration Property Documenter](https://github.com/rodnansol/spring-configuration-property-documenter)
but for pure Spring Framework-based projects (not Spring Boot)

[Project site](docs/index.html)

## Annotation processor "spring-properties-processor"

This annotation processor scans Spring Framework annotations to search for properties placeholders `${...}`
and appends properties metadata into file `META-INF/spring-configuration-metadata.json` which is in [Spring Boot configuration
metadata format](https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html).

```xml
<dependency>
    <groupId>io.github.tia-ru</groupId>
    <artifactId>spring-properties-processor</artifactId>
    <version>LATEST</version>
    <optional>true</optional>
</dependency>
```
The processor accepts compiler's `-A` option: 
- `org.springframework.configurationprocessor.additionalMetadataLocations` - comma separated list of paths where search for
   `META-INF/additional-spring-configuration-metadata.json` file to merge into `META-INF/spring-configuration-metadata.json`

## Maven plugin "spring-properties-maven-plugin"
This plugin has 2 goals.

### Goal"generate-xml-properties-metadata"
The goal scans spring xml-configuration files to search for properties placeholders `${...}`
and appends properties metadata into file `META-INF/spring-configuration-metadata.json`

```xml
<plugin>
    <groupId>io.github.tia-ru</groupId>
    <artifactId>spring-properties-maven-plugin</artifactId>
    <version>LATEST</version>
    <executions>
        <execution>
            <id>generate-xml-properties-metadata</id>
            <goals>
                <goal>generate-xml-properties-metadata</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
The goal binds to `generate-resources` phase by default.

Goals' parameters:
- `xmlLocations` -  List of root directories to search for spring xml-files. Maven module resource directories by default.
- `metadataDir` - A directory where the generated `spring-configuration-metadata.json` file will be saved.
                  Default value: `${project.build.outputDirectory}/META-INF`

#### XML property description
Since: 0.2

To add a description for property in xml configuration file, put comment right before a tag with property.
Write property name at line start (whitespaces before the name are ignored) then separator char
(`:`, or `-`, or new line, or whitespaces), then description.

Separate descriptions of different properties with a blank line.

Write `@deprecated` word in a description to mark a property as deprecated.
The text following `@deprecated` is the reason for the deprecation.

If a property is specified multiple times in different xml files and
has different descriptions, the generated report will contain duplicates of the property
 
>**Limitation:** Xml property type is always reported as `String`.


Example:

```xml
<bean>
    <!-- app.timeout - property description -->
    <property name="timeout" value="${app.timeout:60}"/>
    
    <!--
     Arbitrary comment that is not part of a property description.
     app.title.prefix:
        Multiline 
        description.
        @deprecated Do not use the property anymore
     
     app.title: empty line before is separator           
     
     Arbitrary comment that is not part of a property description.
    -->
    
    <property name="title" value="${app.title.prefix:}${app.title:MyApp}"/>
</bean>
```
### Goal "generate-and-aggregate-documents"

This goal searches `META-INF/spring-configuration-metadata.json` files from the specified sources
and render them into one single human-readable document containing all properties documentation.

The goal is extension of "[Spring Configuration Property Documenter](https://github.com/rodnansol/spring-configuration-property-documenter)'s"
same named goal. For information on usage and configuration, please refer to its documentation.

This goal has just one additional parameter `<inputArtifacts>`. Artifacts specified by the parameter are also included 
in addition to the `<inputs>` parameter to search for `META-INF/spring-configuration-metadata.json`:

- `DEPENDS_ON_MODULES` _(default)_ - Only project modules that are explicitly in the current module dependencies list (external libraries are not included).
 
- `MODULES` - All modules in aggregated project (without external libraries). Metadata generation must run after all modules has been compiled.
  To ensure this, current module should have a dependency on the last compiled module (as war-module usually has).

- `DEPENDENCIES` - Project modules and external libraries that are in transitive dependencies of current module


```xml
<plugin>
    <groupId>io.github.tia-ru</groupId>
    <artifactId>spring-properties-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <inherited>false</inherited>
            <id>generate-docs-markdown</id>
            <goals>
                <goal>generate-and-aggregate-documents</goal>
            </goals>
        </execution>
        <configuration>
            <outputFile>${project.build.directory}/project-properties</outputFile>                 
            <type>MARKDOWN</type>
            <failOnMissingInput>false</failOnMissingInput>
            <markdownCustomization>
                <includeUnknownGroup>false</includeUnknownGroup>
                <removeEmptyGroups>true</removeEmptyGroups>                        
            </markdownCustomization>                    
            <inputArtifacts>MODULES</inputArtifacts>
        </configuration>
    </executions>                
</plugin>
```
The goal binds to `prepare-package` phase by default.

### Goal "compile-and-aggregate-documents"
Same as `generate-and-aggregate-documents` but launch forked `compile` phase for child modules before.
It's usable in aggregator module for `site` phase to trigger 'generate-xml-properties-metadata' goal
and 'spring-properties-processor' annotation processor to produce metadata and then document before site generation.

The goal binds to `pre-site` phase by default.

### Project Site
[Read next](docs/spring-properties-maven-plugin/plugin-info.html)