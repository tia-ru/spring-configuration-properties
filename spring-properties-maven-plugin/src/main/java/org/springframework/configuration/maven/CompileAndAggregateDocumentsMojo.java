package org.springframework.configuration.maven;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Same as {@code 'generate-and-aggregate-documents'} goal, but compile child modules before.
 * It's usable in aggregator module for 'site' phase to trigger 'generate-xml-properties-metadata' goal
 * and 'spring-properties-processor' annotation processor to produce metadata and then document before site generation.
 */
@Mojo(name = CompileAndAggregateDocumentsMojo.GOAL_NAME
        , defaultPhase = LifecyclePhase.PRE_SITE
        , requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
        //, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
        , aggregator = true
        , threadSafe = true
)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class CompileAndAggregateDocumentsMojo extends GenerateAndAggregateDocumentsMojo{
    protected static final String GOAL_NAME = "compile-and-aggregate-documents";
}
