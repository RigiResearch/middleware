package com.rigiresearch.middleware.coordinator.templates;

import com.rigiresearch.middleware.metamodels.hcl.Resource;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A comparator for resources.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class ResourceComparator implements Comparator<Resource> {

    @Override
    public int compare(final Resource first, final Resource second) {
        return ResourceComparator.qualifiedName(first)
            .compareTo(ResourceComparator.qualifiedName(second));
    }

    /**
     * Returns the fully qualified name of a resource.
     * @param resource The resource
     * @return A non-null string
     */
    private static String qualifiedName(final Resource resource) {
        return Stream.of(
            resource.getSpecifier(),
            resource.getType(),
            resource.getName()
        )
            .map(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining("."));
    }

}
