package org.springframework.configuration.maven.xpp3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Xpp3DomEx extends Xpp3Dom {

    protected String comment;

    public Xpp3DomEx(String name) {
        super(name);
    }

    public Xpp3DomEx(Xpp3Dom src) {
        this( src, src.getName() );
    }

    public Xpp3DomEx(Xpp3Dom src, String name) {
        super(src, name);
        if (src instanceof Xpp3DomEx) {
            comment = ((Xpp3DomEx) src).comment;
        }
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
