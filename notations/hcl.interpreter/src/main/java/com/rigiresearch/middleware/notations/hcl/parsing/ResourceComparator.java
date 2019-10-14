package com.rigiresearch.middleware.notations.hcl.parsing;

import com.rigiresearch.middleware.metamodels.hcl.Resource;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A resource comparator.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class ResourceComparator
    implements Comparator<Resource>, Serializable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 5683409137718871664L;

    /**
     * Default initial capacity for maps/collections.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * The orders.
     */
    private final Map<String, Integer> orders;

    /**
     * Default constructor.
     */
    public ResourceComparator() {
        this.orders = ResourceComparator.initialize();
    }

    /**
     * Initialize the resource orders.
     * @return A non-null non-empty map
     */
    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private static Map<String, Integer> initialize() {
        final Map<String, Integer> map =
            new HashMap<>(ResourceComparator.INITIAL_CAPACITY);
        map.put("variable-output", -1);
        map.put("variable-provider", -1);
        map.put("variable-data", -1);
        map.put("variable-resource", -1);
        map.put("output-variable", 1);
        map.put("output-provider", 1);
        map.put("output-data", 1);
        map.put("output-resource", 1);
        map.put("provider-variable", 1);
        map.put("provider-output", 1);
        map.put("provider-data", -1);
        map.put("provider-resource", -1);
        map.put("data-variable", 1);
        map.put("data-output", 1);
        map.put("data-provider", 1);
        map.put("data-resource", -1);
        map.put("resource-variable", 1);
        map.put("resource-output", 1);
        map.put("resource-provider", 1);
        map.put("resource-data", 1);
        return map;
    }

    @Override
    public int compare(final Resource first, final Resource second) {
        final int order;
        final String key = String.format(
            "%s-%s",
            first.getSpecifier(),
            second.getSpecifier()
        );
        if (this.orders.containsKey(key)) {
            order = this.orders.get(key);
        } else {
            // They are the same or contain unknown specifiers
            order = ResourceComparator.compareByFqn(first, second);
        }
        return order;
    }

    /**
     * Compares using the resources' fully qualified name.
     * @param first The first resource
     * @param second The second resource
     * @return Either -1, 0 or 1
     */
    private static int compareByFqn(final Resource first, final Resource second) {
        final int order;
        final int specifier =
            first.getSpecifier().compareTo(second.getSpecifier());
        if (specifier == 0
            && first.getType() != null
            && second.getType() != null) {
            final int type = first.getType().compareTo(second.getType());
            if (type == 0) {
                order = first.getName().compareTo(second.getName());
            } else {
                order = type;
            }
        } else if (specifier == 0) {
            order = first.getName().compareTo(second.getName());
        } else {
            order = specifier;
        }
        return order;
    }
}
