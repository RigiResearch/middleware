package com.rigiresearch.middleware.notations.hcl.parsing;

import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SpecificationSet}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Tag("integration")
final class SpecificationSetTest {

    /**
     * Empty specification array.
     */
    private static final Specification[] EMPTY = new Specification[0];

    /**
     * Default size for collections/maps.
     */
    private static final int DEFAULT_SIZE = 10;

    @Test
    void testCorrectness() throws IOException, HclParsingException {
        final HclParser parser = new HclParser();
        final String[] files = {"variables.tf", "provider.tf"};
        final Map<URI, Specification> map = new HashMap<>(SpecificationSetTest.DEFAULT_SIZE);
        final Resource vars = parser.resource(URI.createFileURI(files[0]));
        final Resource provider = parser.resource(URI.createFileURI(files[1]));
        vars.load(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(files[0]),
            Collections.emptyMap()
        );
        provider.load(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(files[1]),
            Collections.emptyMap()
        );
        map.put(vars.getURI(), parser.parse(vars));
        map.put(provider.getURI(), parser.parse(provider));
        final SpecificationSet set = new SpecificationSet(
            map.values().toArray(SpecificationSetTest.EMPTY)
        );
        Assertions.assertEquals(
            map.values().stream()
                .map(Specification::getResources)
                .map(EList::size)
                .reduce(Integer::sum)
                .get(),
            set.unified().getResources().size(),
            "Unexpected number of resources"
        );
        for (final Map.Entry<URI, Specification> entry
            : set.getMapping().entrySet()) {
            Assertions.assertTrue(
                EcoreUtil.equals(map.get(entry.getKey()), entry.getValue()),
                "Unexpected URI-Specification mapping"
            );
        }
    }

}
