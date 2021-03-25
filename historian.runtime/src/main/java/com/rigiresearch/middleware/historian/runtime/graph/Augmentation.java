package com.rigiresearch.middleware.historian.runtime.graph;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Property;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A mapping property to include a list of inputs in the resulting document.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlType(
    name = "augmentation",
    namespace = Graph.NAMESPACE
)
@EqualsAndHashCode(callSuper = false)
@Getter
@ToString(of = "inputs")
public final class Augmentation extends Property implements Comparable<Property> {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7774337181736685009L;

    /**
     * The list of input names.
     */
    @XmlAttribute
    private Set<String> inputs;

    /**
     * Empty constructor.
     */
    public Augmentation() {
        this(new TreeSet<>());
    }

    /**
     * Primary constructor.
     * @param inputs The names of the inputs to include in the result
     */
    public Augmentation(final Set<String> inputs) {
        super();
        this.inputs = new TreeSet<>(inputs);
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
