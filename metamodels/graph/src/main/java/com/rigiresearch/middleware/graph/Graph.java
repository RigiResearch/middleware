package com.rigiresearch.middleware.graph;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.Marshaller;
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
        "http://www.rigiresearch.com/middleware/graph/1.0.0";

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
     * Finds dependents of a given node.
     * @param node The graph node
     * @return A set of dependent nodes
     */
    public Set<Graph.Node> dependencies(final Graph.Node node) {
        return this.nodes.stream()
            .filter(temp -> {
                return temp.parameters.stream()
                    .filter(param -> param instanceof Graph.Input)
                    .map(Graph.Input.class::cast)
                    .anyMatch(input -> node.equals(input.getSource()));
            })
            .collect(Collectors.toSet());
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
    public static final class Node
        implements Serializable, Comparable<Graph.Node> {

        /**
         * A serial version UID.
         */
        private static final long serialVersionUID = -3483678145906372051L;

        /**
         * Object to recognize whether a node is based on a template.
         */
        private static final Graph.Node TEMPLATE_PILL =
            new Graph.Node("", null, Collections.emptySet());

        /**
         * A unique name within the graph.
         */
        @XmlID
        @XmlAttribute(required = true)
        private String name;

        /**
         * A node on which this node is based.
         */
        @XmlIDREF
        @XmlAttribute
        private Graph.Node template;

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
            this("", Graph.Node.TEMPLATE_PILL, new TreeSet<>());
        }

        /**
         * Secondary constructor.
         * @param name A unique name within the graph
         * @param parameters Parameters to this node
         */
        public Node(final String name, final Set<Graph.Parameter> parameters) {
            this(name, Graph.Node.TEMPLATE_PILL, parameters);
        }

        /**
         * Primary constructor.
         * @param name A unique name within the graph
         * @param template A node on which this node is based
         * @param parameters Parameters to this node
         */
        public Node(final String name, final Graph.Node template,
            final Set<Graph.Parameter> parameters) {
            this.name = name;
            this.template = template;
            this.parameters = new TreeSet<>(parameters);
        }

        /**
         * Setups the template pill before marshall.
         * @param marshaller The XML marshaller
         */
        @SuppressWarnings({
            "PMD.NullAssignment",
            "PMD.UnusedFormalParameter"
        })
        private void beforeMarshal(final Marshaller marshaller) {
            if (this.template == Node.TEMPLATE_PILL) {
                this.template = null;
            }
        }

        /**
         * Setups the template pill after marshal.
         * @param marshaller The XML marshaller
         */
        @SuppressWarnings("PMD.UnusedFormalParameter")
        private void afterMarshal(final Marshaller marshaller) {
            if (this.template == null) {
                this.template = Graph.Node.TEMPLATE_PILL;
            }
        }

        /**
         * Compares this object to another.
         * @param other The other object
         * @return Whether this object should be placed before or after the
         *  other object
         */
        @Override
        public int compareTo(final Graph.Node other) {
            return this.name.compareTo(other.name);
        }

        /**
         * A unique name within the graph.
         * @return The name
         */
        public String getName() {
            return this.name;
        }

        /**
         * A node on which this node is based.
         * @return The template node or null
         */
        public Graph.Node getTemplate() {
            return this.template;
        }

        /**
         * Whether this node is based on a template.
         * @return Whether this node is based on a template.
         */
        public boolean isTemplateBased() {
            return this.template != Graph.Node.TEMPLATE_PILL;
        }

        /**
         * A set of parameters.
         * @param merge Whether to merge the parameters for template-based nodes
         * @return The set of parameters
         */
        public Set<Graph.Parameter> getParameters(final boolean merge) {
            Set<Graph.Parameter> set = new HashSet<>(this.parameters.size());
            if (merge && this.isTemplateBased()) {
                // Get the inherited parameters
                final Set<Graph.Parameter> inherited =
                    this.template.getParameters(true);
                final Map<String, Graph.Input> inputs = inherited
                    .stream()
                    .filter(Graph.Input.class::isInstance)
                    .map(Graph.Input.class::cast)
                    .collect(
                        Collectors.toMap(Graph.Input::getName, Function.identity())
                    );
                final Map<String, Graph.Output> outputs = inherited
                    .stream()
                    .filter(Graph.Output.class::isInstance)
                    .map(Graph.Output.class::cast)
                    .collect(
                        Collectors.toMap(Graph.Output::getName, Function.identity())
                    );
                // Add/replace the parameters based on this node's own parameters
                this.parameters.stream()
                    .filter(Graph.Input.class::isInstance)
                    .map(Graph.Input.class::cast)
                    .forEach(input -> inputs.put(input.getName(), input));
                this.parameters.stream()
                    .filter(Graph.Output.class::isInstance)
                    .map(Graph.Output.class::cast)
                    .forEach(output -> outputs.put(output.getName(), output));
                set.addAll(inputs.values());
                set.addAll(outputs.values());
            } else {
                set = this.parameters;
            }
            return set;
        }

    }

    /**
     * A node parameter.
     */
    @XmlTransient
    public abstract static class Parameter
        implements Serializable, Comparable<Graph.Parameter> {

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
        public int compareTo(final Graph.Parameter other) {
            return this.name.compareTo(other.name);
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
         * Object to recognize whether an input has a source.
         */
        private static final Graph.Node SOURCE_PILL = new Graph.Node();

        /**
         * The value associated with this input. It is either a primitive value
         * or a reference to another node's output.
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
            this("", "", Graph.Input.SOURCE_PILL);
        }

        /**
         * Secondary constructor.
         * @param name The name of this input
         * @param value The value associated with this input (a primitive value)
         */
        public Input(final String name, final String value) {
            this(name, value, Graph.Input.SOURCE_PILL);
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
         * Setups the source pill before marshall.
         * @param marshaller The XML marshaller
         */
        @SuppressWarnings({
            "PMD.NullAssignment",
            "PMD.UnusedFormalParameter"
        })
        private void beforeMarshal(final Marshaller marshaller) {
            if (this.source == Graph.Input.SOURCE_PILL) {
                this.source = null;
            }
        }

        /**
         * Setups the source pill after marshal.
         * @param marshaller The XML marshaller
         */
        @SuppressWarnings("PMD.UnusedFormalParameter")
        private void afterMarshal(final Marshaller marshaller) {
            if (this.source == null) {
                this.source = Graph.Input.SOURCE_PILL;
            }
        }

        /**
         * Whether this input is based on a source.
         * @return Whether this node is based on a source.
         */
        public boolean hasSource() {
            return this.source != Graph.Input.SOURCE_PILL;
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
