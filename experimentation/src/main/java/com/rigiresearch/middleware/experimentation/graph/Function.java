package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Parameter;
import com.rigiresearch.middleware.graph.Property;
import java.util.Set;
import javax.xml.bind.annotation.XmlType;

/**
 * A graph node of type Function.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "function",
    namespace = Graph.NAMESPACE
)
public final class Function extends Node {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -3533094053390810672L;

    /**
     * Empty constructor.
     */
    public Function() {
        super();
    }

    /**
     * Secondary constructor.
     * @param name A unique name within the graph
     * @param parameters Inputs of this function
     * @param metadata Configuration properties
     */
    public Function(final String name, final Set<Parameter> parameters,
        final Set<Property> metadata) {
        super(name, parameters, metadata);
    }

}
