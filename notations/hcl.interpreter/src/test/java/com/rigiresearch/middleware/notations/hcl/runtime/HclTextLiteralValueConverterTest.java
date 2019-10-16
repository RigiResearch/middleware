package com.rigiresearch.middleware.notations.hcl.runtime;

import com.google.inject.Inject;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import com.rigiresearch.middleware.notations.hcl.tests.HclInjectorProvider;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link HclTextLiteralValueConverterTest}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(HclInjectorProvider.class)
@Tag("integration")
final class HclTextLiteralValueConverterTest {

    /**
     * The HCL qualified name provider.
     */
    @Inject
    private IQualifiedNameProvider provider;

    @Test
    void testFullyQualifiedName() throws IOException, HclParsingException {
        final String file = "variables.tf";
        final HclParser parser = new HclParser();
        final Resource vars = parser.resource(
            URI.createFileURI(file)
        );
        vars.load(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(file),
            Collections.emptyMap()
        );
        final Specification spec = parser.parse(vars);
        for (final EObject object: spec.getResources()) {
            Assertions.assertFalse(
                this.provider.getFullyQualifiedName(object)
                    .toString(".")
                    .contains("\""),
                "Qualified name should not contain quotes"
            );
        }
    }

}
