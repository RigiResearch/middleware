package com.rigiresearch.middleware.historian.runtime.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Property;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A mapping property to transform a document based on a given Xpath selector.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "transformation",
    namespace = Graph.NAMESPACE
)
@EqualsAndHashCode(callSuper = false)
@Getter
@SuppressWarnings({
    "checkstyle:MemberName",
    "checkstyle:ParameterName"
})
@ToString(of = {"selector", "multivalued", "groupByInput"})
public final class Transformation extends Property implements Comparable<Property> {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 5959779891950118373L;

    /**
     * The Xpath selector.
     */
    @XmlAttribute
    private String selector;

    /**
     * Whether the Xpath selector locates many values.
     */
    @XmlAttribute
    private Boolean multivalued;

    /**
     * Optional. Whether the transformed elements must be grouped by a
     * particular input value.
     */
    @XmlAttribute
    private String groupByInput;

    /**
     * Empty constructor.
     */
    public Transformation() {
        this("", Boolean.FALSE, "");
    }

    /**
     * Primary constructor.
     * @param selector The Xpath selector
     * @param multivalued Whether the Xpath selector locates many values
     * @param groupByInput Optional. Whether the transformed elements must be
     *  grouped by a particular input value
     */
    public Transformation(final String selector, final boolean multivalued,
        final String groupByInput) {
        super();
        this.selector = selector;
        this.multivalued = multivalued;
        this.groupByInput = groupByInput;
    }

    /**
     * Whether this transformation should group by input.
     * @return A boolean value
     */
    public boolean shouldGroupByInput() {
        return this.groupByInput != null && !this.groupByInput.isEmpty();
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
        if (this.groupByInput.isEmpty()) {
            this.groupByInput = null;
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
        if (this.groupByInput == null) {
            this.groupByInput = "";
        }
    }

    @Override
    public int compareTo(final Property property) {
        final int result;
        if (this.equals(property)) {
            result = 0;
        } else {
            result = 1;
        }
        return result;
    }

}
