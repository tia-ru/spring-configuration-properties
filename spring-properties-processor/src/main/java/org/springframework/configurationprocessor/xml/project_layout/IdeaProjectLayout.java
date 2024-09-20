package org.springframework.configurationprocessor.xml.project_layout;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class IdeaProjectLayout implements ProjectLayout {
    @Override
    public boolean support(ProcessingEnvironment processor) {
        return getModuleDir(processor) != null;
    }

    @Override
    public List<Path> xmlLocations(ProcessingEnvironment processor) {
        // D:\t\idea-proj-layout\out\production\idea-proj-layout
        // D:\t\idea-proj-layout\out\production\mod1

        String moduleDir = getModuleDir(processor).toString();
        Path path;
        path = Path.of(moduleDir, "resources");
        if (path.toFile().exists()) {
            return List.of(path);
        }
        return List.of();
    }

    private Path getModuleDir(ProcessingEnvironment processor) {
        try {
            Filer filer = processor.getFiler();

            // D:\t\idea-proj-layout\out\production\idea-proj-layout\tmp
            // D:\t\idea-proj-layout\out\production\mod1\tmp
            FileObject classOutput = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "tmp");

            String classOutputPathString = classOutput.getName();
            int i = classOutputPathString.indexOf(File.separator + "out" + File.separator + "production" + File.separator);
            if (i >=0) {
                Path projectPath = Path.of(classOutputPathString.substring(0, i));
                Path classOutputPath = Path.of(classOutputPathString).getParent();
                if (projectPath.getFileName().equals(classOutputPath.getFileName())){
                    //root module
                    return projectPath;
                } else {
                    // sub module
                    return projectPath.resolve(classOutputPath.getFileName());
                    //return Path.of(projectPath.toString(), classOutputPath.getFileName().toString());
                }
            }
        } catch (IOException ignore) {
        }
        return null;
    }
}
