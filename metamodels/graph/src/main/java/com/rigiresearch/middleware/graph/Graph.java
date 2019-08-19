package com.rigiresearch.middleware.graph;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A DAG with input/output dependencies.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 * @param <T> The type of node
 */
@XmlRootElement(
    name = "graph",
    namespace = Graph.NAMESPACE
)
public class Graph<T extends Node> implements Serializable {

    /**
     * The XML namespace.
     */
    public static final String NAMESPACE =
        "http://www.rigiresearch.com/middleware/graph/1.0.0";

    /**
     * A serial version UID.
     */
    private static final long serialVersionUID = 6108711703700072578L;

    /**
     * The set of nodes.
     */
    @XmlElement(name = "node")
    private Set<T> nodes;

    /**
     * Empty constructor.
     */
    public Graph() {
        this(new TreeSet<>());
    }

    /**
     * Default constructor.
     * @param nodes The set of nodes
     */
    public Graph(final Set<T> nodes) {
        this.nodes = new TreeSet<>(nodes);
    }

    /**
     * Finds dependent nodes of a given node.
     * @param node The graph node
     * @return A set of dependent nodes
     */
    public Set<T> dependents(final T node) {
        if (!this.nodes.contains(node)) {
            throw new IllegalArgumentException(
                String.format("Node %s was not found in this graph", node)
            );
        }
        return this.nodes.stream()
            .filter(temp ->
                temp.getParameters(true).stream()
                    .filter(Input.class::isInstance)
                    .map(Input.class::cast)
                    .anyMatch(input -> node.equals(input.getSource()))
            )
            .collect(Collectors.toSet());
    }

    /**
     * Whether the given object is equivalent to this graph.
     * @param object Another object
     * @return True if the object is instance of this class and their nodes are
     *  equivalent.
     */
    @Override
    public boolean equals(final Object object) {
        final boolean equivalent;
        if (this == object) {
            equivalent = true;
        } else if (object instanceof Graph) {
            final Graph<?> graph = (Graph<?>) object;
            equivalent = this.getNodes().equals(graph.getNodes());
        } else {
            equivalent = false;
        }
        return equivalent;
    }

    /**
     * The hash code of this graph.
     * @return A hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getNodes());
    }

    /**
     * The set of nodes.
     * The set of nodes. This set can be used to add and remove nodes.
     * @return A set
     */
    public Set<T> getNodes() {
        return this.nodes;
    }

}
