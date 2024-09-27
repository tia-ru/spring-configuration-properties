package org.springframework.configuration.maven.patch;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.rodnansol.core.generator.template.data.PropertyDeprecation;

public class PropertyDeprecationPatch extends PropertyDeprecation {
    public PropertyDeprecationPatch(@Nullable String reason, @Nullable String replacement) {
        super(reason, replacement);
    }

    @Override
    public String toString() {
        if (getReason() == null && getReplacement() == null ) {
            return "Deprecated";
        } else if (getReason() == null ) {
            return "Deprecated. Replacement: " + getReplacement();
        } else if (getReplacement() == null ) {
            return "Reason: " + getReason();
        } else {
            return "Reason: " + getReason() + ". Replacement: " + getReplacement();
        }
    }
}
