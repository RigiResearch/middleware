package com.rigiresearch.middleware.metamodels.hcl;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EObject;

/**
 * Converts elements of the HCL model to a unique id. This is used to match
 * elements from HCL models being compared for merging.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class HclMatchEngineQualifiedNameConverter {

    /**
     * The character ussed to separate the qualified name segments.
     */
    private static final CharSequence SEPARATOR = ".";

    /**
     * Creates a fully qualified name for the given element. In case the element
     * is of an unknown type, it returns null.
     * @param element The EObject instance
     * @return A possibly null or empty string
     */
    public String fullyQualifiedName(final EObject element) {
        final String identifier;
        if (element instanceof Resource) {
            identifier = ((Resource) element).getFullyQualifiedName();
        } else if (element instanceof Comment) {
            identifier = this.fullyQualifiedName((Comment) element);
        } else if (element instanceof Dictionary) {
            identifier = this.fullyQualifiedName((Dictionary) element);
        } else if (element instanceof NameValuePair) {
            identifier = this.fullyQualifiedName((NameValuePair) element);
        } else {
            identifier = null;
        }
        return identifier;
    }

    /**
     * Creates a fully qualified name for the given element.
     * @param element The EObject instance
     * @return A non-null, non-empty qualified name
     */
    private String fullyQualifiedName(final Comment element) {
        return HclMatchEngineQualifiedNameConverter.fullyQualifiedName(
            this.fullyQualifiedName(element.eContainer()),
            element.eClass().getName()
        );
    }

    /**
     * Creates a fully qualified name for the given element.
     * @param element The EObject instance
     * @return A non-null, non-empty qualified name
     */
    private String fullyQualifiedName(final Dictionary element) {
        return HclMatchEngineQualifiedNameConverter.fullyQualifiedName(
            this.fullyQualifiedName(element.eContainer()),
            element.eClass().getName(),
            element.getName()
        );
    }

    /**
     * Creates a fully qualified name for the given element.
     * @param element The EObject instance
     * @return A non-null, non-empty qualified name
     */
    private String fullyQualifiedName(final NameValuePair element) {
        return HclMatchEngineQualifiedNameConverter.fullyQualifiedName(
            this.fullyQualifiedName(element.eContainer()),
            element.getName()
        );
    }

    /**
     * Puts together an array of id components into a fully qualified name.
     * @param components The components of the qualified name
     * @return A qualified-name-formatted string
     */
    private static String fullyQualifiedName(final String... components) {
        return Arrays.stream(components)
            .filter(value -> value != null && !value.isEmpty())
            .collect(
                Collectors.joining(HclMatchEngineQualifiedNameConverter.SEPARATOR)
            );
    }

}
