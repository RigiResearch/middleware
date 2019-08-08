package com.rigiresearch.middleware.graph;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.eclipse.persistence.oxm.annotations.XmlPath;

/**
 * An input parameter.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "input",
    namespace = Graph.NAMESPACE,
    propOrder = {"name", "value", "source"}
)
public class Input extends Parameter {

    /**
     * A serial version UID.
     */
    private static final long serialVersionUID = -7875573841695028057L;

    /**
     * Object to recognize whether an input has a source.
     */
    private static final Node SOURCE_PILL = new Node();

    /**
     * The value associated with this input. It is either a primitive value
     * or a reference to another node's output.
     */
    @XmlPath(".")
    @XmlJavaTypeAdapter(InputValueAdapter.class)
    private String value;

    /**
     * The containing node of the referenced output.
     */
    @XmlIDREF
    @XmlAttribute
    private Node source;

    /**
     * Empty constructor.
     */
    public Input() {
        this("", "", Input.SOURCE_PILL);
    }

    /**
     * Secondary constructor.
     * @param name The name of this input
     * @param value The value associated with this input (a primitive value)
     */
    public Input(final String name, final String value) {
        this(name, value, Input.SOURCE_PILL);
    }

    /**
     * Primary constructor.
     * @param name The name of this input
     * @param value A reference to another node's output
     * @param source The containing node of the referenced output
     */
    public Input(final String name, final String value,
        final Node source) {
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
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void beforeMarshal(final Marshaller marshaller) {
        if (this.source == Input.SOURCE_PILL) {
            this.source = null;
        }
    }

    /**
     * Setups the source pill after marshal.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void afterMarshal(final Marshaller marshaller) {
        if (this.source == null) {
            this.source = Input.SOURCE_PILL;
        }
    }

    /**
     * Whether this input is based on a source.
     * @return Whether this node is based on a source.
     */
    public boolean hasSource() {
        return this.source != Input.SOURCE_PILL;
    }

    /**
     * Whether this input is NOT based on a source.
     * @return Whether this node is NOT based on a source.
     */
    public boolean hasConcreteValue() {
        return this.source == Input.SOURCE_PILL;
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
    public Node getSource() {
        return this.source;
    }

}
