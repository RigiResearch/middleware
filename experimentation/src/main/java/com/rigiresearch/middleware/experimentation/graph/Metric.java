package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Parameter;
import com.rigiresearch.middleware.graph.Property;
import java.util.Set;
import javax.xml.bind.annotation.XmlType;

/**
 * A graph node of type Metric.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "metric",
    namespace = Graph.NAMESPACE
)
public final class Metric extends Node {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -3527056482233576021L;

    /**
     * Empty constructor.
     */
    public Metric() {
        super();
    }

    /**
     * Secondary constructor.
     * @param name A unique name within the graph
     * @param parameters Dependencies (inputs) of this metric
     * @param metadata Configuration properties
     */
    public Metric(final String name, final Set<Parameter> parameters,
        final Set<Property> metadata) {
        super(name, parameters, metadata);
    }

}
