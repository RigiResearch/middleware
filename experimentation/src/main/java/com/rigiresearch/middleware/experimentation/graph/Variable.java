package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Property;
import java.util.Collections;
import java.util.Set;
import javax.xml.bind.annotation.XmlType;

/**
 * A graph node of type Variable.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "variable",
    namespace = Graph.NAMESPACE
)
public final class Variable extends Node {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7076449201934620461L;

    /**
     * Empty constructor.
     */
    public Variable() {
        super();
    }

    /**
     * Secondary constructor.
     * @param name A unique name within the graph
     * @param metadata Configuration properties
     */
    public Variable(final String name, final Set<Property> metadata) {
        super(name, Collections.emptySet(), metadata);
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append(this.getClass().getSimpleName())
            .append("(")
            .append(this.getName())
            .append(", ")
            .append("metadata: ")
            .append(this.getMetadata().toString())
            .append(")")
            .toString();
    }

}
