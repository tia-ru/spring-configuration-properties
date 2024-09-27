package org.springframework.configuration.maven.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.configuration.maven.xpp3.Xpp3DomEx;
import org.springframework.configurationprocessor.helpers.StringUtils;
import org.springframework.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

public class DefaultMetadataEnricher implements MetadataEnricher {

    private static final Pattern PATTERN_CRLF = Pattern.compile("[\\n\\r]");
    private static final Pattern PATTERN_REMAINING = Pattern.compile("[\\s-:]+(.*)");

    private final StringBuilder builder = new StringBuilder(2048);

    @Override
    public void enrich(ItemMetadata metadata, Xpp3DomEx node) {
        String comment = node.getComment();
        if (comment == null) {
            return;
        }

        builder.setLength(0);
        boolean isInDescription = false;
        String name = metadata.getName();
        String[] lines = PATTERN_CRLF.split(comment);
        for (String line : lines) {
            line = line.trim();
            if (!isInDescription) {
                if (line.startsWith(name) ) {
                    isInDescription = true;
                    line = line.substring(name.length());
                    Matcher matcher = PATTERN_REMAINING.matcher(line);
                    if (matcher.matches()) {
                        String remaining = matcher.group(1);
                        if (StringUtils.hasText(remaining)) {
                            builder.append(remaining);
                        }
                    }
                }
            } else {
                isInDescription = StringUtils.hasText(line); //break description on empty line
                if (isInDescription) {
                    if (builder.length() > 0) builder.append(' ');
                    builder.append(line);
                } else {
                    break;
                }
            }
        }

        String result = builder.toString();
        if (result.contains("@deprecated")) {
            metadata.setDeprecation(new ItemDeprecation());
        }

        metadata.setDescription(result);
    }
}
