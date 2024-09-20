package org.springframework.configuration.maven;

public enum InputArtifactsKind {
    /**
     * All aggregator project modules
     */
    MODULES,

    /**
     * Project modules that is in dependency list of current module
     */
    DEPENDS_ON_MODULES,

    /**
     * Project modules and external libraries that are in transitive dependencies of current module
     */
    DEPENDENCIES
}
