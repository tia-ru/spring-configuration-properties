package org.springframework.configurationprocessor.xml.project_layout;

import javax.annotation.processing.ProcessingEnvironment;
import java.nio.file.Path;
import java.util.List;

public interface ProjectLayout {
    boolean support(ProcessingEnvironment processor);
    List<Path> xmlLocations(ProcessingEnvironment processor);
}
