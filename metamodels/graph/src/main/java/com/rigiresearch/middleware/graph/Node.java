package com.rigiresearch.middleware.graph;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * A graph node.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "node",
    namespace = Graph.NAMESPACE
)
public class Node implements Serializable, Comparable<Node> {

    /**
     * A serial version UID.
     */
    private static final long serialVersionUID = -3483678145906372051L;

    /**
     * Object to recognize whether a node is based on a template.
     */
    private static final Node TEMPLATE_PILL =
        new Node("", null, Collections.emptySet(), Collections.emptySet());

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
    private Node template;

    /**
     * Parameters to this node.
     */
    @XmlElements({
        @XmlElement(
            name = "input",
            namespace = Graph.NAMESPACE,
            type = Input.class
        ),
        @XmlElement(
            name = "output",
            namespace = Graph.NAMESPACE,
            type = Output.class
        )
    })
    private Set<Parameter> parameters;

    /**
     * Metadata properties associated with this node.
     */
    @XmlElement(name = "property")
    @XmlElementWrapper(name = "metadata")
    private Set<Property> metadata;

    /**
     * Empty constructor.
     */
    public Node() {
        this("", Node.TEMPLATE_PILL, new TreeSet<>(), new TreeSet<>());
    }

    /**
     * Secondary constructor.
     * @param name A unique name within the graph
     * @param parameters Parameters to this node
     * @param metadata Metadata properties associated with this node
     */
    public Node(final String name, final Set<Parameter> parameters,
        final Set<Property> metadata) {
        this(name, Node.TEMPLATE_PILL, parameters, metadata);
    }

    /**
     * Primary constructor.
     * @param name A unique name within the graph
     * @param template A node on which this node is based
     * @param parameters Parameters to this node
     * @param metadata Metadata properties associated with this node
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public Node(final String name, final Node template,
        final Set<Parameter> parameters, final Set<Property> metadata) {
        this.name = name;
        this.template = template;
        this.parameters = new TreeSet<>(parameters);
        this.metadata = new TreeSet<>(metadata);
    }

    /**
     * Finds the dependencies of this node.
     * @return A set of dependent nodes
     */
    public Set<Node> dependencies() {
        return this.getParameters(true).stream()
            .filter(Input.class::isInstance)
            .map(Input.class::cast)
            .map(Input::getSource)
            .collect(Collectors.toSet());
    }

    /**
     * Setups the template pill before marshall.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.NullAssignment",
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
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
    @SuppressWarnings({
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void afterMarshal(final Marshaller marshaller) {
        if (this.template == null) {
            this.template = Node.TEMPLATE_PILL;
        }
    }

    /**
     * Compares this object to another.
     * @param other The other object
     * @return Whether this object should be placed before or after the
     *  other object
     */
    @Override
    public int compareTo(final Node other) {
        final int result;
        final boolean equal = this.equals(other);
        final int comparison = this.name.compareTo(other.name);
        if (equal) {
            result = 0;
        } else if (comparison < 0 || comparison > 0) {
            result = comparison;
        } else {
            result = 1;
        }
        return result;
    }

    /**
     * Determines whether this and another object are equivalent based on
     * their names.
     * @param object The other object
     * @return Whether the two objects are equivalent
     */
    @Override
    public boolean equals(final Object object) {
        boolean equivalent = false;
        if (object instanceof Node) {
            final Node node = (Node) object;
            equivalent = this.name.equals(node.name)
                && Objects.equals(this.template, node.template)
                && this.parameters.equals(node.parameters)
                && this.metadata.equals(node.metadata);
        }
        return equivalent;
    }

    /**
     * Generates a hash code for the name, template and parameters.
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            this.name,
            this.template,
            this.parameters,
            this.metadata
        );
    }

    /**
     * A string representation of this node.
     * @return A non-null string
     */
    @Override
    public String toString() {
        return new StringBuilder()
            .append(this.getClass().getSimpleName())
            .append("(")
            .append(this.name)
            .append(", ")
            .append("parameters: ")
            .append(
                this.parameters.stream()
                    .map(Parameter::getName)
                    .collect(Collectors.joining(", ", "[", "]"))
            )
            .append(", ")
            .append("metadata: ")
            .append(
                this.metadata.stream()
                    .map(Property::getName)
                    .collect(Collectors.joining(", ", "[", "]"))
            )
            .append(")")
            .toString();
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
     * @return The template node or the template pill
     */
    public Node getTemplate() {
        return this.template;
    }

    /**
     * Whether this node is based on a template.
     * @return Whether this node is based on a template.
     */
    public boolean isTemplateBased() {
        return this.template != Node.TEMPLATE_PILL;
    }

    /**
     * A set of parameters.
     * @param merge Whether to merge the parameters for template-based nodes
     * @return The set of parameters
     */
    public Set<Parameter> getParameters(final boolean merge) {
        Set<Parameter> set = new HashSet<>(this.parameters.size());
        if (merge && this.isTemplateBased()) {
            // Get the inherited parameters
            final Set<Parameter> inherited =
                this.template.getParameters(true);
            final Map<String, Input> inputs = inherited.stream()
                .filter(Input.class::isInstance)
                .map(Input.class::cast)
                .collect(
                    Collectors.toMap(Input::getName, Function.identity())
                );
            final Map<String, Output> outputs = inherited.stream()
                .filter(Output.class::isInstance)
                .map(Output.class::cast)
                .collect(
                    Collectors.toMap(Output::getName, Function.identity())
                );
            // Add/replace the parameters based on this node's own parameters
            this.parameters.stream()
                .filter(Input.class::isInstance)
                .map(Input.class::cast)
                .forEach(input -> inputs.put(input.getName(), input));
            this.parameters.stream()
                .filter(Output.class::isInstance)
                .map(Output.class::cast)
                .forEach(output -> outputs.put(output.getName(), output));
            set.addAll(inputs.values());
            set.addAll(outputs.values());
        } else {
            set = this.parameters;
        }
        return set;
    }

    /**
     * Metadata properties associated with this node.
     * @return A non-null set
     */
    public Set<Property> getMetadata() {
        return this.metadata;
    }
}
