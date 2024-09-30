package org.springframework.configuration.maven.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.configuration.maven.xpp3.Xpp3DomBuilderEx;
import org.springframework.configuration.maven.xpp3.Xpp3DomEx;
import org.springframework.configurationprocessor.helpers.PropertyPlaceholderHelper;
import org.springframework.configurationprocessor.helpers.StringUtils;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

public class XmlMetadataScanner {

    private static final String NAMESPACE_SPRING = "http://www.springframework.org/schema/";
    private static final String VALUE_SEPARATOR = ":";
    private static final PropertyPlaceholderHelper PROPERTY_HELPER = new PropertyPlaceholderHelper("${", "}", VALUE_SEPARATOR, true);
    private static final MetadataEnricher DEFAULT_DESCRIPTION_EXTRACTOR = new DefaultMetadataEnricher();

    private final List<Path> locations;
    private final MetadataEnricher metadataEnricher;

    public XmlMetadataScanner(List<Path> locations, MetadataEnricher metadataEnricher){
        this.locations = locations;
        this.metadataEnricher = metadataEnricher;
    }

    public XmlMetadataScanner(List<Path> locations){
        this(locations, DEFAULT_DESCRIPTION_EXTRACTOR);
    }

    public Set<ItemMetadata> scan() {

        return locations.stream()
                .flatMap(root -> getRootMetadata(root).stream())
                .collect(Collectors.toSet());

    }

    private Set<ItemMetadata> getRootMetadata(Path root) {
        Set<ItemMetadata> rootMetadata;
        if (!root.toFile().exists()) {
            rootMetadata = Set.of();
        } else {

            try (Stream<Path> pathStream = Files.find(root, Integer.MAX_VALUE,
                    ((path, fileAttributes) -> fileAttributes.isRegularFile() && "xml".equals(StringUtils.getFilenameExtension(path.toString()))))) {
                rootMetadata = pathStream.filter(XmlMetadataScanner::isSpringXml)
                        .flatMap(path -> getFileMetadata(path).stream())
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return rootMetadata;
    }

    private static boolean isSpringXml(Path path) {
        try {
            BufferedReader reader = Files.newBufferedReader(path);
            Xpp3DomEx firstTag = Xpp3DomBuilderEx.buildFirstTag(reader);
            return firstTag != null && firstTag.getAttributes() != null
                    && firstTag.getAttributes().values().stream().anyMatch(v -> v.startsWith(NAMESPACE_SPRING));
        } catch (IOException | XmlPullParserException e) {
            //throw new RuntimeException("Error parsing file '" + path + "'", e);
            return false;
        }

    }

    private Set<ItemMetadata> getFileMetadata(Path path) {
        Set<ItemMetadata> fileMetadata = new HashSet<>();
        LinkedBlockingQueue<Xpp3DomEx> queue = new LinkedBlockingQueue<>();
        Xpp3DomEx node;

        try {
            BufferedReader reader = Files.newBufferedReader(path);
            Xpp3DomEx root =  Xpp3DomBuilderEx.buildWithComments(reader);
            if (root == null) return fileMetadata;

            queue.offer(root);
            while ((node = queue.poll()) != null) {

                if (StringUtils.hasText(node.getValue())) {
                    Collection<ItemMetadata> metadata = extractMeta(node.getValue(), path);
                    for (ItemMetadata item : metadata) {
                         metadataEnricher.enrich(item, node);
                    }

                    fileMetadata.addAll(metadata);
                }

                if (node.getAttributes() != null) {
                    for (String value : node.getAttributes().values()) {
                        Collection<ItemMetadata> metadata = extractMeta(value, path);
                        for (ItemMetadata item : metadata) {
                            metadataEnricher.enrich(item, node);
                        }
                        fileMetadata.addAll(metadata);
                    }
                }

                if (node.getChildList() != null) queue.addAll(node.getChildList());
            }
        } catch (IOException | XmlPullParserException e){
            throw new RuntimeException("Unable to process file '" + path +"'. " + e.getMessage(), e);
        }
        return fileMetadata;
    }

    private Collection<ItemMetadata> extractMeta(String value, Path path) {
        Map<String, String> map = PROPERTY_HELPER.extractPlaceholders(value);
        return map.entrySet().stream()
                .map(entry -> {
                    String placeHolder = entry.getKey();
                    String defVal = entry.getValue();
                    ItemMetadata metadata = ItemMetadata.newProperty("", placeHolder, String.class.getCanonicalName(),
                            path.getFileName().toString(), null, null, defVal, null);
                    return metadata;
                }).collect(Collectors.toSet());
    }


 /*   @FunctionalInterface
    private interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    private static <T> T throwingWrapper(ThrowingSupplier<T, Exception> throwingSupplier) {
            try {
                return throwingSupplier.get();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
    }*/

}
