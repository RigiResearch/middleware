package com.rigiresearch.middleware.experimentation.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Parameter;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
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
     * Empty constructor.
     */
    public NodeDependency() {
        super();
    }

    /**
     * Secondary constructor.
     * @param node The referenced node
     */
    public NodeDependency(final Node node, final String name) {
        super(name);
        this.node = node;
    }

    /**
     * The referenced node.
     * @return A possibly null object
     */
    public Node getNode() {
        return this.node;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final NodeDependency that = (NodeDependency) o;
        return Objects.equals(this.node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.node);
    }

    @Override
    public String toString() {
        return String.format("dependency(%s, %s)", this.getName(), this.node);
    }

}
