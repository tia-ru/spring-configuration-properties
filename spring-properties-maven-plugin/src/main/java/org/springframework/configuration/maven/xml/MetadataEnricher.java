package org.springframework.configuration.maven.xml;

import org.springframework.configuration.maven.xpp3.Xpp3DomEx;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

public interface MetadataEnricher {
    void enrich(ItemMetadata metadata, Xpp3DomEx node);
}
