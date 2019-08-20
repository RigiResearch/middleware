package com.rigiresearch.middleware.graph;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

/**
 * A node parameter.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlTransient
public abstract class Parameter
    implements Serializable, Comparable<Parameter> {

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
    public int compareTo(final Parameter other) {
        return this.name.compareTo(other.name);
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
        if (object instanceof Parameter) {
            final Parameter parameter = (Parameter) object;
            equivalent = this.name.equals(parameter.name);
        }
        return equivalent;
    }

    /**
     * Generates a hash code for the name.
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    /**
     * A string representation of this parameter.
     * @return A non-null string
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * A unique name within the set of parameters.
     * @return The name of this parameter
     */
    public String getName() {
        return this.name;
    }

}
