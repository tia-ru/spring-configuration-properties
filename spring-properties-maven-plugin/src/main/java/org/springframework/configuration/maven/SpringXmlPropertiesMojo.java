package org.springframework.configuration.maven;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.configuration.maven.xml.MetadataProcessor;

/**
 * This goal reads all springframework xml configuration files from the given/specified directories
 * and generate or enhance `META-INF/spring-configuration-metadata.json` that will contain all '${}' properties metadata.
 *
 * @author tia
 * @since 0.1
 */
@Mojo(name = SpringXmlPropertiesMojo.GOAL_NAME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
//@Execute(phase = LifecyclePhase.PROCESS_RESOURCES, goal = SpringXmlPropertiesMojo.GOAL_NAME, lifecycle = "default")
public class SpringXmlPropertiesMojo extends AbstractMojo {

    protected static final String GOAL_NAME = "generate-xml-properties-metadata";

    // @Parameter( defaultValue = "${session}", readonly = true )
    /*@Inject
    private MavenSession session;*/

    /**
     * Maven project instance.
     */
    // @Parameter( defaultValue = "${project}", readonly = true )
    @Inject
    private MavenProject project;

    // @Parameter( defaultValue = "${mojoExecution}", readonly = true )
    /*@Inject
    private MojoExecution mojoExecution;*/

    /**
     * Root directories to scan for spring xml-files. All project resource directories by default.
     */
    @Parameter(name = "xmlLocations")
    List<String> xmlLocations;

    /**
     * A directory where the generated 'spring-configuration-metadata.json' file will be saved
     */
    @Parameter(name = "metadataDir", defaultValue = "${project.build.outputDirectory}/META-INF", property = "spring.properties.metadata.directory")
    String metadataDir;

    @Override
    public void execute() {
        getLog().info("generate-xml-properties-metadata");
        List<Path> xmlLocationPaths;
        if (xmlLocations == null || xmlLocations.isEmpty()) {
            xmlLocationPaths = project.getBuild().getResources().stream()
                    .map(r -> Path.of(r.getDirectory()))
                    .collect(Collectors.toList());
        } else {
            xmlLocationPaths = xmlLocations.stream()
                    .map(Path::of)
                    .collect(Collectors.toList());
        }
        MetadataProcessor generator = new MetadataProcessor(xmlLocationPaths, Path.of(metadataDir));
        generator.execute();

    }
}
