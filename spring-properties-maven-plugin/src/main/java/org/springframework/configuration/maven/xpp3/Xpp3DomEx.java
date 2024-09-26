package org.springframework.configuration.maven.xpp3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Xpp3DomEx extends Xpp3Dom {

    public Xpp3DomEx(String name) {
        super(name);
    }

    public Xpp3DomEx(Xpp3Dom src) {
        super(src);
    }

    public Xpp3DomEx(Xpp3Dom src, String name) {
        super(src, name);
    }

    public List<Xpp3DomEx> getChildList() {
        return childList;
    }

    public Set<String> getAttributeNamesSet() {
        return attributes.keySet();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
