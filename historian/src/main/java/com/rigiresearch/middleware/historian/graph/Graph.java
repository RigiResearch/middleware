package com.rigiresearch.middleware.historian.graph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A DAG with input/output dependencies.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlRootElement(namespace = Graph.NAMESPACE)
public final class Graph implements Serializable {

    /**
     * The XML namespace.
     */
    static final String NAMESPACE =
        "http://www.rigiresearch.com/middleware/historian/graph/1.0.0";

    /**
     * A serial version UID.
     */
    private static final long serialVersionUID = 6108711703700072578L;

    /**
     * The set of nodes.
     */
    @XmlElementWrapper(name = "nodes")
    @XmlElement(name = "node")
    private Set<Graph.Node> nodes;

    /**
     * The set of edges.
     */
    @XmlElementWrapper(name = "edges")
    @XmlElement(name = "edge")
    private Set<Graph.Edge> edges;

    /**
     * Empty constructor.
     */
    public Graph() {
        this(new HashSet<>(0), new HashSet<>(0));
    }

    /**
     * Default constructor.
     * @param nodes The set of nodes
     * @param edges The set of edges
     */
    public Graph(final Set<Graph.Node> nodes, final Set<Graph.Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * The set of nodes.
     * @return A set
     */
    public Set<Graph.Node> getNodes() {
        return this.nodes;
    }

    /**
     * A set of edges.
     * @return The set of edges
     */
    public Set<Graph.Edge> getEdges() {
        return this.edges;
    }

    /**
     * A graph node.
     */
    @XmlType(namespace = Graph.NAMESPACE)
    public static final class Node implements Serializable {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -3483678145906372051L;

        /**
         * A unique name within the graph.
         */
        @XmlID
        @XmlAttribute(
            name = "id",
            required = true
        )
        private String name;

        /**
         * Parameters to this node.
         */
        @XmlElements({
            @XmlElement(
                name = "input",
                namespace = Graph.NAMESPACE,
                type = Graph.Input.class
            ),
            @XmlElement(
                name = "output",
                namespace = Graph.NAMESPACE,
                type = Graph.Output.class
            )
        })
        private Set<Graph.Parameter> parameters;

        /**
         * Empty constructor.
         */
        public Node() {
            this("", new HashSet<>(0));
        }

        /**
         * Default constructor.
         * @param name A unique name within the graph
         * @param parameters Parameters to this node
         */
        public Node(final String name, final Set<Graph.Parameter> parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        /**
         * A unique name within the graph.
         * @return The name
         */
        public String getName() {
            return this.name;
        }

        /**
         * A set of parameters.
         * @return The set of parameters
         */
        public Set<Graph.Parameter> getParameters() {
            return this.parameters;
        }

    }

    /**
     * A node parameter.
     */
    @XmlTransient
    public abstract static class Parameter implements Serializable {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = 835469922313051647L;

        /**
         * The name of this parameter.
         */
        @XmlID
        @XmlAttribute(required = true)
        private String name;

        /**
         * Empty constructor.
         */
        public Parameter() {
            this("");
        }

        /**
         * Default constructor.
         * @param name The name of this parameter
         */
        public Parameter(final String name) {
            this.name = name;
        }

        /**
         * A unique name within the set of parameters.
         * @return The name of this parameter
         */
        public String getName() {
            return this.name;
        }

    }

    /**
     * An input parameter.
     */
    @XmlType(
        name = "input",
        namespace = Graph.NAMESPACE
    )
    public static final class Input extends Graph.Parameter {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -7875573841695028057L;

        /**
         * Empty constructor.
         */
        public Input() {
            this("");
        }

        /**
         * Default constructor.
         * @param name The name of this input
         */
        public Input(final String name) {
            super(name);
        }

    }

    /**
     * An output parameter.
     */
    @XmlType(
        name = "output",
        namespace = Graph.NAMESPACE,
        propOrder = {"name", "selector"}
    )
    public static final class Output extends Graph.Parameter {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -3911876210238970060L;

        /**
         * An Xpath selector.
         */
        @XmlAttribute(required = true)
        private String selector;

        /**
         * Empty constructor.
         */
        public Output() {
            this("", "");
        }

        /**
         * Default constructor.
         * @param name The name of this input
         * @param selector The Xpath selector
         */
        public Output(final String name, final String selector) {
            super(name);
            this.selector = selector;
        }

        /**
         * An xpath selector.
         * @return The selector
         */
        public String getSelector() {
            return this.selector;
        }

    }

    /**
     * An edge between two nodes.
     */
    @XmlType(namespace = Graph.NAMESPACE)
    @SuppressWarnings("PMD.DataClass")
    public static final class Edge implements Serializable {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = 7206583134109784056L;

        /**
         * A source node.
         */
        @XmlIDREF
        @XmlAttribute(required = true)
        private Graph.Node source;

        /**
         * An input from the source node.
         */
        @XmlIDREF
        @XmlAttribute(required = true)
        private Graph.Input input;

        /**
         * A target node.
         */
        @XmlIDREF
        @XmlAttribute(required = true)
        private Graph.Node target;

        /**
         * An output from the target node.
         */
        @XmlIDREF
        @XmlAttribute(required = true)
        private Graph.Output output;

        /**
         * Empty constructor.
         */
        public Edge() {
            // Nothing to do here
        }

        /**
         * Default constructor.
         * @param source A source node
         * @param input An input from the source node
         * @param target A target node
         * @param output An output from the target node
         */
        @SuppressWarnings("checkstyle:ParameterNumber")
        public Edge(final Graph.Node source, final Graph.Input input,
            final Graph.Node target, final Graph.Output output) {
            this.source = source;
            this.input = input;
            this.target = target;
            this.output = output;
        }

        /**
         * A source node.
         * @return The source
         */
        public Graph.Node getSource() {
            return this.source;
        }

        /**
         * An input from the source node.
         * @return The input
         */
        public Graph.Input getInput() {
            return this.input;
        }

        /**
         * A target node.
         * @return The target
         */
        public Graph.Node getTarget() {
            return this.target;
        }

        /**
         * An output from the target node.
         * @return The output
         */
        public Graph.Output getOutput() {
            return this.output;
        }

    }

}
