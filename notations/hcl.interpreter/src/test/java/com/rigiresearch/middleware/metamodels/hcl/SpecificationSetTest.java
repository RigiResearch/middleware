package com.rigiresearch.middleware.metamodels.hcl;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
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
     * Default error message.
     */
    private static final String MESSAGE = "Unexpected number of resources";

    /**
     * Resources to use in the test cases.
     */
    private static final String[] FILES = {"variables.tf", "provider.tf"};

    /**
     * Default size for collections/maps.
     */
    private static final int DEFAULT_SIZE = 10;

    @Test
    void testUnificationCorrectness() throws IOException, HclParsingException {
        final Map<URI, Specification> map = SpecificationSetTest.instantiate();
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
            SpecificationSetTest.MESSAGE
        );
        for (final Map.Entry<URI, Specification> entry
            : set.getMapping().entrySet()) {
            Assertions.assertTrue(
                EcoreUtil.equals(map.get(entry.getKey()), entry.getValue()),
                "Unexpected URI-Specification mapping"
            );
        }
    }

    @Test
    void testUpdate() throws IOException, HclParsingException {
        final Map<URI, Specification> map = SpecificationSetTest.instantiate();
        final SpecificationSet set = new SpecificationSet(
            map.values().toArray(SpecificationSetTest.EMPTY)
        );
        final Specification unified = set.unified();
        final com.rigiresearch.middleware.metamodels.hcl.Resource resource =
            HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("resource");
        resource.setType("virtual_machine");
        resource.setName("vm_1");
        resource.setValue(HclFactory.eINSTANCE.createDictionary());
        unified.getResources().add(resource);
        set.update(unified);
        final int count = map.values().stream()
            .map(Specification::getResources)
            .map(EList::size)
            .reduce(Integer::sum)
            .get();
        Assertions.assertEquals(
            1 + count,
            set.unified().getResources().size(),
            SpecificationSetTest.MESSAGE
        );
        Assertions.assertTrue(
            set.getMapping().containsKey(URI.createFileURI("main.tf")),
            "Incorrect file mapping"
        );
        unified.getResources().remove(resource);
        set.update(unified);
        Assertions.assertEquals(
            count,
            set.unified().getResources().size(),
            SpecificationSetTest.MESSAGE
        );
    }

    /**
     * Instantiate the specifications based on the test resources.
     * @return A mapping URI-specification containing the instantiated objects
     * @throws IOException If there's an I/O error
     * @throws HclParsingException If there's a parsing error
     */
    private static Map<URI, Specification> instantiate()
        throws IOException, HclParsingException {
        final HclParser parser = new HclParser();
        final Resource vars = parser.resource(
            URI.createFileURI(SpecificationSetTest.FILES[0])
        );
        final Resource provider = parser.resource(
            URI.createFileURI(SpecificationSetTest.FILES[1])
        );
        vars.load(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(SpecificationSetTest.FILES[0]),
            Collections.emptyMap()
        );
        provider.load(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(SpecificationSetTest.FILES[1]),
            Collections.emptyMap()
        );
        final Map<URI, Specification> map =
            new HashMap<>(SpecificationSetTest.DEFAULT_SIZE);
        map.put(vars.getURI(), parser.parse(vars));
        map.put(provider.getURI(), parser.parse(provider));
        return map;
    }

}
