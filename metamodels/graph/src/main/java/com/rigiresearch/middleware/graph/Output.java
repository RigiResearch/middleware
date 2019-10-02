package com.rigiresearch.middleware.graph;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * An output parameter.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "output",
    namespace = Graph.NAMESPACE,
    propOrder = {"name", "selector"}
)
public class Output extends Parameter {

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
     * Whether the Xpath selector locates many values.
     */
    @XmlAttribute
    private Boolean multivalued;

    /**
     * Empty constructor.
     */
    public Output() {
        this("", "", false);
    }

    /**
     * Secondary constructor. It assumes that the selector is not
     * multivalued.
     * @param name The name of this input
     * @param selector The Xpath selector
     */
    public Output(final String name, final String selector) {
        this(name, selector, false);
    }

    /**
     * Default constructor.
     * @param name The name of this input
     * @param selector The Xpath selector
     * @param multivalued Whether the Xpath selector locates many values.
     */
    public Output(final String name, final String selector,
        final boolean multivalued) {
        super(name);
        this.selector = selector;
        this.multivalued = multivalued;
    }

    /**
     * Setups the multivalued attribute before marshall.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.NullAssignment",
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void beforeMarshal(final Marshaller marshaller) {
        if (!this.multivalued) {
            this.multivalued = null;
        }
    }

    /**
     * Setups the multivalued attribute after marshal.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void afterMarshal(final Marshaller marshaller) {
        if (this.multivalued == null) {
            this.multivalued = Boolean.FALSE;
        }
    }

    /**
     * An xpath selector.
     * @return The selector
     */
    public String getSelector() {
        return this.selector;
    }

    /**
     * Whether the Xpath selector locates many values.
     * @return Whether the Xpath selector locates many values.
     */
    public Boolean isMultivalued() {
        return this.multivalued;
    }

}
