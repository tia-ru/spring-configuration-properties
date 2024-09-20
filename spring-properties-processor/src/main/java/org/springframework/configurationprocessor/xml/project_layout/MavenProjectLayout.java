package org.springframework.configurationprocessor.xml.project_layout;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MavenProjectLayout implements ProjectLayout {
    @Override
    public boolean support(ProcessingEnvironment processor) {
        return getModuleDir(processor) != null;
    }

    @Override
    public List<Path> xmlLocations(ProcessingEnvironment processor) {

        String moduleDir = getModuleDir(processor).toString();
        Path path;
        path = Path.of(moduleDir, "target/classes");
        if (path.toFile().exists()) {
            return List.of(path);
        }
        path = Path.of(moduleDir, "src/main/resources");
        if (path.toFile().exists()) {
            return List.of(path);
        }
        return List.of();
    }

    private Path getModuleDir(ProcessingEnvironment processor) {
        try {
            Filer filer = processor.getFiler();

            // D:\proto\spring-configuration-processor\test-project\target\classes\tmp
            FileObject classOutput = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "tmp");

            String classOutputPathString = classOutput.getName();
            int i = classOutputPathString.indexOf(File.separator + "target" + File.separator);
            if (i >=0) {
                return Path.of(classOutputPathString.substring(0, i));
            }
        } catch (IOException ignore) {
        }
        return null;
    }
}
