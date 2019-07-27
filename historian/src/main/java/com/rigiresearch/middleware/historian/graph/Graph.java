package com.rigiresearch.middleware.historian.graph;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
@XmlRootElement(
    name = "monitors",
    namespace = Graph.NAMESPACE
)
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
    @XmlElement(name = "monitor")
    private Set<Graph.Node> nodes;

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
    public Graph(final Set<Graph.Node> nodes) {
        this.nodes = new TreeSet<>(nodes);
    }

    /**
     * The set of nodes.
     * @return A set
     */
    public Set<Graph.Node> getNodes() {
        return this.nodes;
    }

    /**
     * A graph node.
     */
    @XmlType(namespace = Graph.NAMESPACE)
    public static final class Node implements Serializable, Comparable {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -3483678145906372051L;

        /**
         * A unique name within the graph.
         */
        @XmlID
        @XmlAttribute(required = true)
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
            this("", new TreeSet<>());
        }

        /**
         * Default constructor.
         * @param name A unique name within the graph
         * @param parameters Parameters to this node
         */
        public Node(final String name, final Set<Graph.Parameter> parameters) {
            this.name = name;
            this.parameters = new TreeSet<>(parameters);
        }

        /**
         * Compares this object to another.
         * @param other The other object
         * @return Whether this object should be placed before or after the
         *  other object
         */
        @Override
        public int compareTo(final Object other) {
            int value = 0;
            if (other instanceof Graph.Node) {
                final Graph.Node node = (Graph.Node) other;
                value = this.getName().compareTo(node.getName());
            }
            return value;
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
    public abstract static class Parameter implements Serializable, Comparable {

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
         * Compares this object to another.
         * @param other The other object
         * @return Whether this object should be placed before or after the
         *  other object
         */
        @Override
        public int compareTo(final Object other) {
            int value = 0;
            if (other instanceof Graph.Parameter) {
                final Graph.Parameter parameter = (Graph.Parameter) other;
                value = this.getName().compareTo(parameter.getName());
            }
            return value;
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
        namespace = Graph.NAMESPACE,
        propOrder = {"name", "value", "source"}
    )
    public static final class Input extends Graph.Parameter {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -7875573841695028057L;

        /**
         * The value associated with this input. It is either a primitive value or
         * a reference to another node's output.
         */
        @XmlAttribute(required = true)
        private String value;

        /**
         * The containing node of the referenced output.
         */
        @XmlIDREF
        @XmlAttribute
        private Graph.Node source;

        /**
         * Empty constructor.
         */
        public Input() {
            this("", "", new Graph.Node());
        }

        /**
         * Secondary constructor.
         * @param name The name of this input
         * @param value The value associated with this input (a primitive value)
         */
        public Input(final String name, final String value) {
            this(name, value, new Graph.Node());
        }

        /**
         * Primary constructor.
         * @param name The name of this input
         * @param value A reference to another node's output
         * @param source The containing node of the referenced output
         */
        public Input(final String name, final String value,
            final Graph.Node source) {
            super(name);
            this.value = value;
            this.source = source;
        }

        /**
         * A value.
         * @return The value associated with this input.
         */
        public String getValue() {
            return this.value;
        }

        /**
         * A node.
         * @return The containing node of the referenced output
         */
        public Graph.Node getSource() {
            return this.source;
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

}
