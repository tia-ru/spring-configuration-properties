package org.springframework.configurationprocessor.xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.configurationprocessor.helpers.PropertyPlaceholderHelper;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

public class XmlMetadataScanner {

    private static final String NAMESPACE_SPRING = "http://www.springframework.org/schema/";
    private static final String VALUE_SEPARATOR = ":";
    private static final PropertyPlaceholderHelper PROPERTY_HELPER = new PropertyPlaceholderHelper("${", "}", VALUE_SEPARATOR, true);

    private final List<Path> locations;

    public XmlMetadataScanner(List<Path> locations){
        this.locations = locations;
    }

    public Set<ItemMetadata> scan() {

        return locations.stream()
                .flatMap(root -> getRootMetadata(root).stream())
                .collect(Collectors.toSet());

       /* return locations.stream()
                .flatMap(root -> throwingWrapper(() -> Files.find(root, Integer.MAX_VALUE,
                        (path, fileAttributes) -> fileAttributes.isRegularFile() && ".xml".equals(getFileExtension(path))))
                ).filter(path -> throwingWrapper(() -> Files.lines(path))
                        .limit(20)
                        .anyMatch(s -> s.contains("http://www.springframework.org/schema/"))
                ).flatMap(path -> throwingWrapper(() -> Files.lines(path))
                        .flatMap(line -> PROPERTY_HELPER.extractPlaceholders(line).entrySet().stream())
                        .map(entry -> {
                            String placeHolder = entry.getKey();
                            String defVal = entry.getValue();
                            ItemMetadata metadata = ItemMetadata.newProperty("", placeHolder, String.class.getCanonicalName(),
                                    path.getFileName().toString(), null, null, defVal, null);
                            return metadata;
                        })
                ).collect(Collectors.toSet());*/
    }

    private Set<ItemMetadata> getRootMetadata(Path root) {
        Set<ItemMetadata> rootMetadata;
        try (Stream<Path> pathStream = Files.find(root, Integer.MAX_VALUE,
                ((path, fileAttributes) -> fileAttributes.isRegularFile() && ".xml".equals(getFileExtension(path))))) {
            rootMetadata = pathStream.filter(XmlMetadataScanner::isSpringXml)
                    .flatMap(path -> getFileMetadata(path).stream())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rootMetadata;
    }

    private static boolean isSpringXml(Path path) {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.limit(20).anyMatch(s -> s.contains(NAMESPACE_SPRING));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<ItemMetadata> getFileMetadata(Path path) {
        Set<ItemMetadata> fileMetadata;
        try (Stream<String> lines = Files.lines(path)) {
            fileMetadata = lines.flatMap(
                    line -> PROPERTY_HELPER.extractPlaceholders(line).entrySet().stream()
            ).map(entry -> {
                String placeHolder = entry.getKey();
                String defVal = entry.getValue();
                ItemMetadata metadata = ItemMetadata.newProperty("", placeHolder, String.class.getCanonicalName(),
                        path.getFileName().toString(), null, null, defVal, null);
                return metadata;
            }).collect(Collectors.toSet());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return fileMetadata;
    }


    /** get file extension
     *
     * @param file
     * @return
     */
    private String getFileExtension(Path file){

        Path name = file.getFileName();
        if (name == null)
            return null;
        String filename = name.toString();
        int dot = filename.lastIndexOf('.');
        if ((dot < 0) || (dot == (filename.length()-1)))
            return null;
        return filename.substring(dot).toLowerCase();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    private static <T> T throwingWrapper(ThrowingSupplier<T, Exception> throwingSupplier) {
            try {
                return throwingSupplier.get();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
    }
}
