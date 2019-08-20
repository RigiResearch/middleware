package com.rigiresearch.middleware.graph;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

/**
 * A metadata property.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "property",
    namespace = Graph.NAMESPACE
)
public class Property implements Serializable, Comparable<Property> {

    /**
     * A serial version UID.
     */
    private static final long serialVersionUID = -7965710324825643412L;

    /**
     * A unique name for this property within a metadata element.
     */
    @XmlID
    @XmlAttribute
    private String name;

    /**
     * The value of this property.
     */
    @XmlAttribute
    private String value;

    /**
     * Empty constructor.
     */
    public Property() {
        this("", "");
    }

    /**
     * Primary constructor.
     * @param name A unique name for this property within a metadata element
     * @param value The value of this property
     */
    public Property(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Setups the name and value before marshall.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.NullAssignment",
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void beforeMarshal(final Marshaller marshaller) {
        if (this.name.isEmpty()) {
            this.name = null;
        }

        if (this.value.isEmpty()) {
            this.value = null;
        }
    }

    /**
     * Setups the name and value after marshal.
     * @param marshaller The XML marshaller
     */
    @SuppressWarnings({
        "PMD.UnusedFormalParameter",
        "PMD.UnusedPrivateMethod"
    })
    private void afterMarshal(final Marshaller marshaller) {
        if (this.name == null) {
            this.name = "";
        }
        if (this.value == null) {
            this.value = "";
        }
    }

    /**
     * Compares this object to another.
     * @param other The other object
     * @return Whether this object should be placed before or after the
     *  other object
     */
    @Override
    public int compareTo(final Property other) {
        return this.name.compareTo(other.name);
    }

    /**
     * Whether another object is equivalent to this property.
     * @param object The other object
     * @return True if the other object is a {@link Property and their name and
     *  value are equivalent}
     */
    @Override
    public boolean equals(final Object object) {
        final boolean equivalent;
        if (this == object) {
            equivalent = true;
        } else if (object instanceof Property) {
            final Property property = (Property) object;
            equivalent = this.getName().equals(property.getName())
                && this.getValue().equals(property.getValue());
        } else {
            equivalent = false;
        }
        return equivalent;
    }

    /**
     * The hash code.
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getName(), this.getValue());
    }

    /**
     * A unique name for this property within a metadata element.
     * @return A non-null string
     */
    public String getName() {
        return this.name;
    }

    /**
     * The value of this property.
     * @return A non-null string
     */
    public String getValue() {
        return this.value;
    }
}
