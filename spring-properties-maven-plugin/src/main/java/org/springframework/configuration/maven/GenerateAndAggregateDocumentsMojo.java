package org.springframework.configuration.maven;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.rodnansol.core.generator.resolver.InputFileResolutionStrategy;
import org.rodnansol.core.generator.resolver.MetadataInputResolverContext;
import org.rodnansol.core.generator.template.TemplateType;
import org.rodnansol.core.generator.template.compiler.TemplateCompilerFactory;
import org.rodnansol.core.generator.template.customization.AsciiDocTemplateCustomization;
import org.rodnansol.core.generator.template.customization.HtmlTemplateCustomization;
import org.rodnansol.core.generator.template.customization.MarkdownTemplateCustomization;
import org.rodnansol.core.generator.template.customization.TemplateCustomization;
import org.rodnansol.core.generator.template.customization.XmlTemplateCustomization;
import org.rodnansol.core.generator.template.handlebars.HandlebarsTemplateCompiler;
import org.rodnansol.core.generator.writer.CombinedInput;
import org.rodnansol.core.generator.writer.CreateAggregationCommand;
import org.rodnansol.core.generator.writer.CustomTemplate;
import org.rodnansol.core.generator.writer.postprocess.PropertyGroupFilterService;
import org.rodnansol.core.project.ProjectFactory;
import org.rodnansol.maven.AggregationMojoInput;
import org.springframework.configuration.maven.patch.AggregationDocumenterPatch;
import org.springframework.configuration.maven.patch.MetadataReaderPatch;
import org.springframework.configurationprocessor.helpers.StringUtils;

/**
 * This goal reads all the given `META-INF/spring-configuration-metadata.json` files from the given/specified sources 
 * and it will be generating one single document that will contain all documentation created by the given sources. 
 * This goal is good for a multi module setup, it is able to read multiple files and aggregate them.
 *
 * @author nandorholozsnyak
 * @author tia
 * @since 0.1
 */
@Mojo(name = GenerateAndAggregateDocumentsMojo.GOAL_NAME
        , defaultPhase = LifecyclePhase.PREPARE_PACKAGE
        , requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
        //, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
        , aggregator = true
        , threadSafe = true
)

public class GenerateAndAggregateDocumentsMojo extends AbstractMojo {

    protected static final String GOAL_NAME = "generate-and-aggregate-documents";

    /**
     * Maven project instance.
     */
    //@Parameter(defaultValue = "${project}", required = true, readonly = true)
    @Inject
    MavenProject project;

    @Inject
    private MavenSession session;

    /**
     * Main header section name.
     *
     * @since 0.1
     */
    @Parameter(property = "name", required = true, defaultValue = "${project.name}")
    String name;

    /**
     * Main module description.
     *
     * @since 0.1
     */
    @Parameter(property = "description", defaultValue = "${project.description}")
    String description;

    /**
     * Type of the document.
     * <p>
     * The following template types are supported:<br/>
     * {@code <ul>
     *     <li>MARKDOWN</li>}<br/>
     * {@code <li>ADOC</li>}<br/>
     * {@code <li>HTML</li>}<br/>
     * {@code <li>XML (Since 0.2.0)</li>
     * </ul>}
     *
     * @since 0.1
     */
    @Parameter(property = "type", defaultValue = "MARKDOWN")
    TemplateType type;

    /**
     * HTML template customization object to configure the template.
     *
     * @since 0.1
     */
    @Parameter(property = "htmlCustomization")
    HtmlTemplateCustomization htmlCustomization;

    /**
     * Markdown template customization object to configure the template.
     *
     * @since 0.1
     */
    @Parameter(property = "markdownCustomization")
    MarkdownTemplateCustomization markdownCustomization;

    /**
     * AsciiDoc template customization object to configure the template.
     *
     * @since 0.1
     */
    @Parameter(property = "asciiDocCustomization")
    AsciiDocTemplateCustomization asciiDocCustomization;

    /**
     * XML template customization object to configure the template.
     *
     * @since 0.1
     */
    @Parameter(property = "xmlCustomization")
    XmlTemplateCustomization xmlCustomization;

    /**
     * Input files and additional configuration.
     *
     * @since 0.1
     */
    @Parameter(property = "inputs")
    List<AggregationMojoInput> inputs;

    /**
     * In addition to the {@code 'inputs'} parameter, artifacts specified by the value are also included <br/>
     * {@code <ul>}
     * {@code <li><b>DEPENDS_ON_MODULES</b>} - {@code <i>(default)</i>}  Only project modules that are explicitly in the current module dependencies (external libraries are not included).</li><br/>
     * {@code <li><b>MODULES</b>} - All modules in aggregated project (without external libraries). Metadata generation must run after all modules has been compiled.
     *      To ensure this, current module should have a dependency on the last compiled module (as war-module usually has).
     * </li><br/>
     * {@code <li><b>DEPENDENCIES</b>} - Project modules and external libraries that are in transitive dependencies of current module</li>
     * {@code </ul>}
     *
     *  @since 0.1
     */
    @Parameter(defaultValue = "DEPEND_ON_MODULES")
    InputArtifactsKind inputArtifacts;

    /**
     * Output file.
     *
     * @since 0.1
     */
    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/project-properties", required = true)
    File outputFile;

    /**
     * Template compiler class's fully qualified name .
     * <p>
     * With this option you can use your own template compiler implementation if the default {@link HandlebarsTemplateCompiler}. based one is not enough.
     *
     * @since 0.1
     */
    @Parameter(property = "templateCompilerName")
    String templateCompilerName = TemplateCompilerFactory.getDefaultCompilerName();


    /**
     * Custom header template file.
     *
     * @since 0.1
     */
    @Parameter(property = "headerTemplate")
    String headerTemplate;

    /**
     * Custom content template file.
     *
     * @since 0.1
     */
    @Parameter(property = "contentTemplate")
    String contentTemplate;

    /**
     * Custom footer template file.
     *
     * @since 0.1
     */
    @Parameter(property = "footerTemplate")
    String footerTemplate;

    /**
     * Define if the process should fail if the given input file is not found.
     *
     * @since 0.1
     */
    @Parameter(property = "failOnMissingInput", defaultValue = "true")
    boolean failOnMissingInput;

    @Inject
    protected ProjectBuilder projectBuilder;

    public GenerateAndAggregateDocumentsMojo() {
      //  getLog().debug("Start");
    }

    @Override
    public void execute() {
        List<AggregationMojoInput> allInputs = collectInputs();

        AggregationDocumenterPatch aggregationDocumenter = new AggregationDocumenterPatch(MetadataReaderPatch.INSTANCE,
                TemplateCompilerFactory.getInstance(templateCompilerName), MetadataInputResolverContext.INSTANCE,
                PropertyGroupFilterService.INSTANCE);
        CreateAggregationCommand createAggregationCommand = createAggregationCommand(allInputs);
        aggregationDocumenter.createDocumentsAndAggregate(createAggregationCommand);

    }

    private List<AggregationMojoInput> collectInputs() {
        List<AggregationMojoInput> allInputs = new ArrayList<>();
        if (inputs != null) {
            allInputs.addAll(inputs);
        }
        //project.getDependencies(); parent.getModel().getModules()
        //project.getProjectReferences(); project.getParent().getParent().getBasedir(); project.getParent().getModel().getParent()

        List<MavenProject> modules = null;
        if (inputArtifacts != null) {
            if (inputArtifacts == InputArtifactsKind.DEPENDS_ON_MODULES) {

                modules = new ArrayList<>(project.getProjectReferences().values());

            } else if (inputArtifacts == InputArtifactsKind.MODULES) {
                MavenProject root = getAggregatorRoot();
                modules = session.getProjects();
                if (!project.equals(root)) {
                    modules = collectChildModules(root);
                }

            }
        }

        if (modules != null) {

            //System.out.println("===== PROJECTS ===========================");
            List<AggregationMojoInput> projectModulesInputs = modules.stream()
                    // to avoid UOE after MetadataReader.readPropertiesAsPropertyGroupList returns unmodifiable List.of()
                    //.peek(p -> System.out.println(p.getName()))
                    .filter(p -> hasMetadata(p.getBasedir()))
                    .map(p -> {
                        AggregationMojoInput input = new AggregationMojoInput();
                        input.setInput(p.getBasedir());
                        input.setName(p.getName());
                        input.setDescription(p.getOriginalModel().getDescription());
                        return input;
                    }).collect(Collectors.toList());
            allInputs.addAll(projectModulesInputs);
        }
        if (inputArtifacts != null && inputArtifacts == InputArtifactsKind.DEPENDENCIES) {
           // System.out.println("===== DEPENDENCY ===========================");
            List<AggregationMojoInput> jars = project.getArtifacts().stream()
                    //.peek(p -> System.out.println(p.getArtifactId()))
                    .filter(a -> hasMetadata(a.getFile()))
                    .map(a -> {
                        AggregationMojoInput input = new AggregationMojoInput();
                        input.setInput(a.getFile());
                        input.setName(a.getArtifactId());
                        //input.setDescription();
                        return input;
                    }).collect(Collectors.toList());
            allInputs.addAll(jars);

        }
        return allInputs;
    }

    private MavenProject getAggregatorRoot() {
        MavenProject root = project;
        MavenProject parent = project;
        while (parent != null) {
            parent = parent.getParent();
            if (parent != null && parent.getBasedir() != null && parent.getModules() != null) {
                root = parent;
            }
        }
        return root;
    }

    private List<MavenProject> collectChildModules(MavenProject root) {
        boolean problems = false;
        List<MavenProject> projects = new ArrayList<>();
        if (projectBuilder == null) return projects;

        Log logger = getLog();
        try {
            List<ProjectBuildingResult> results = projectBuilder.build(Collections.singletonList(root.getFile()), true,
                    session.getProjectBuildingRequest());

            for (ProjectBuildingResult result : results) {
                projects.add(result.getProject());

                if (!result.getProblems().isEmpty() && logger.isWarnEnabled()) {
                    logger.warn("");
                    logger.warn("Some problems were encountered while building the effective model for "
                            + result.getProject().getId());

                    for (ModelProblem problem : result.getProblems()) {
                        String loc = ModelProblemUtils.formatLocation(problem, result.getProjectId());
                        logger.warn(problem.getMessage() + (StringUtils.hasLength(loc) ? " @ " + loc : ""));
                    }

                    problems = true;
                }
            }

        } catch (ProjectBuildingException e) {
            logger.error(e);
        }
        return projects;
    }

    private boolean hasMetadata(File f) {
        try {
            org.rodnansol.core.project.maven.MavenProject docMavenProject = ProjectFactory.ofMavenProject(project.getBasedir(),
                    project.getName(), project.getModules());
            return MetadataInputResolverContext.INSTANCE.getInputStreamFromFile(docMavenProject, f,
                    InputFileResolutionStrategy.RETURN_EMPTY).available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private CreateAggregationCommand createAggregationCommand(List<AggregationMojoInput> allInputs) {
        List<CombinedInput> combinedInputs = allInputs.stream()
                .map(this::mapToCombinedInput)
                .sorted(Comparator.comparing(combinedInput -> combinedInput.getSectionName().toLowerCase()))
                .collect(Collectors.toList());
        org.rodnansol.core.project.maven.MavenProject mavenProject = ProjectFactory.ofMavenProject(project.getBasedir(), name,
                project.getModules());

        File fixedOutputFile = fixFileNameExtention(outputFile, type);

        CreateAggregationCommand createAggregationCommand = new CreateAggregationCommand(mavenProject, name, combinedInputs, type,
                getActualTemplateCustomization(), fixedOutputFile);
        createAggregationCommand.setDescription(description);
        createAggregationCommand.setCustomTemplate(new CustomTemplate(headerTemplate, contentTemplate, footerTemplate));
        createAggregationCommand.setFailOnMissingInput(failOnMissingInput);
        return createAggregationCommand;
    }



    private CombinedInput mapToCombinedInput(AggregationMojoInput aggregationMojoInput) {
        CombinedInput combinedInput = new CombinedInput(aggregationMojoInput.getInput(), aggregationMojoInput.getName(),
                aggregationMojoInput.getDescription());
        combinedInput.setExcludedGroups(aggregationMojoInput.getExcludedGroups());
        combinedInput.setIncludedGroups(aggregationMojoInput.getIncludedGroups());
        combinedInput.setIncludedProperties(aggregationMojoInput.getIncludedProperties());
        combinedInput.setExcludedProperties(aggregationMojoInput.getExcludedProperties());
        return combinedInput;
    }

    private TemplateCustomization getActualTemplateCustomization() {
        switch (type) {
        case MARKDOWN:
            return markdownCustomization;
        case ADOC:
            return asciiDocCustomization;
        case HTML:
            return htmlCustomization;
        case XML:
            return xmlCustomization;
        }
        throw new IllegalStateException("There is no template customization set for the current run");
    }

    /*private Result<? extends ProjectDependencyGraph> buildGraph(MavenSession session, MavenExecutionResult result) {
        Log logger = getLog();
        DefaultGraphBuilder graphBuilder;
        Result<? extends ProjectDependencyGraph> graphResult = graphBuilder.build(session);
        for (ModelProblem problem : graphResult.getProblems()) {
            if (problem.getSeverity() == ModelProblem.Severity.WARNING) {
                logger.warn(problem.toString());
            } else {
                logger.error(problem.toString());
            }
        }

        if (!graphResult.hasErrors()) {
            ProjectDependencyGraph projectDependencyGraph = graphResult.get();
            *//*session.setProjects(projectDependencyGraph.getSortedProjects());
            session.setAllProjects(projectDependencyGraph.getAllProjects());
            session.setProjectDependencyGraph(projectDependencyGraph);*//*
        }

        return graphResult;
    }*/

    private static File fixFileNameExtention(File outputFile, TemplateType type) {
        File fixedOutputFile;
        Path outPath = Path.of(outputFile.toURI());
        String ext = "";
        switch (type){
        case MARKDOWN:
            ext = "md";
            break;
        case ADOC:
            ext = "adoc";
            break;
        case HTML:
            ext = "html";
            break;
        case XML:
            ext = "xml";
            break;
        }

        if (!ext.equals(StringUtils.getFilenameExtension(outPath.toString()))) {
            Path newFileName = Path.of(outPath.getFileName().toString() + '.' + ext);
            outPath = outPath.getParent().resolve(newFileName);
        }

        fixedOutputFile = outPath.toFile();
        return fixedOutputFile;
    }

}
