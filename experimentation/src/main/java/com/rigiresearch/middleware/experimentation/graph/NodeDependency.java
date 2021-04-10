package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Parameter;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A node dependency.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "dependency",
    namespace = Graph.NAMESPACE
)
public final class NodeDependency extends Parameter {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 2429693426638206573L;

    /**
     * The input node.
     */
    @XmlIDREF
    @XmlAttribute(required = true)
    private Node node;

    /**
     * This parameter's name (ignored).
     */
    @XmlTransient
    private String name;

    /**
     * Empty constructor.
     */
    public NodeDependency() {
        super("");
    }

    /**
     * Secondary constructor.
     * @param node The referenced node
     */
    public NodeDependency(final Node node) {
        super(node.getName());
        this.node = node;
        this.name = node.getName();
    }

    /**
     * The referenced node.
     * @return A possibly null object
     */
    public Node getNode() {
        return this.node;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
