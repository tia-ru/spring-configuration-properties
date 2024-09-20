package org.springframework.configuration.maven.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.springframework.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

public class MetadataProcessor {
    private MetadataStore metadataStore;

    private MetadataCollector metadataCollector;
    List<Path> xmlLocations;
    private final Path metadataDirectory;

    public MetadataProcessor(List<Path> xmlLocations, Path metadataDirectory) {

        this.xmlLocations = xmlLocations;
        this.metadataDirectory = metadataDirectory;
    }

    public void execute() {
        this.metadataStore = new MetadataStore(metadataDirectory);
        this.metadataCollector = new MetadataCollector(this.metadataStore.readMetadata());
        processXml();
        Set<ItemMetadata> groups = metadataCollector.generateBlankGroups();
        metadataCollector.add(groups);
        writeMetadata();
    }

    private void processXml() {
        XmlMetadataScanner xmlMetadataScanner = new XmlMetadataScanner(xmlLocations);
        Set<ItemMetadata> metadataSet = xmlMetadataScanner.scan();
        metadataCollector.add(metadataSet);
    }


    protected ConfigurationMetadata writeMetadata() {
        try {
            ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
            //metadata = mergeAdditionalMetadata(metadata);
            if (!metadata.getItems().isEmpty()) {
                this.metadataStore.writeMetadata(metadata);
                return metadata;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
