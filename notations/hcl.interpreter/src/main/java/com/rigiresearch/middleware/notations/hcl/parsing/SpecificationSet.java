package com.rigiresearch.middleware.notations.hcl.parsing;

import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
import com.rigiresearch.middleware.metamodels.hcl.Resource;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Specification set for HCL source code coming from separate files.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class SpecificationSet {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SpecificationSet.class);

    /**
     * Default size for collections/maps.
     */
    private static final int DEFAULT_SIZE = 10;

    /**
     * The specifications composing this set.
     */
    @Getter
    private final Specification[] elements;

    /**
     * A URI-Resource mapping to recreate the source code files.
     */
    private final Map<URI, Resource> mapping;

    /**
     * Default constructor.
     * @param specifications The specifications composing this set
     */
    public SpecificationSet(final Specification... specifications) {
        this.elements = specifications.clone();
        this.mapping = new HashMap<>(SpecificationSet.DEFAULT_SIZE);
    }

    /**
     * A unified specification composed of resources coming from this set's
     * specifications.
     * @return A non-null specification
     */
    public Specification unified() {
        // May throw DanglingHREFException if serialized, but it's better than null
        // See Specification#parse(Resource)
        Specification specification = HclFactory.eINSTANCE.createSpecification();
        try {
            specification = new HclParser().parse("");
        } catch (final HclParsingException | IOException exception) {
            SpecificationSet.LOGGER.error(
                "Error creating empty specification",
                exception
            );
        }
        for (final Specification tmp : this.elements) {
            for (final Resource resource : tmp.getResources()) {
                this.mapping.put(resource.eResource().getURI(), resource);
            }
        }
        // Separate to prevent ConcurrentModificationException
        for (final Specification tmp : this.elements) {
            specification.getResources().addAll(tmp.getResources());
        }
        return specification;
    }

    /**
     * A map of URI-specification elements.
     * Note: Each specification must have a unique URI.
     * @return A non-null map
     */
    public Map<URI, Specification> getMapping() {
        final Map<URI, Specification> map =
            new HashMap<>(SpecificationSet.DEFAULT_SIZE);
        for (final Specification specification : this.elements) {
            if (map.containsKey(specification.eResource().getURI())) {
                throw new IllegalStateException("URI must be unique per specification");
            } else {
                map.put(specification.eResource().getURI(), specification);
            }
        }
        return map;
    }

}
