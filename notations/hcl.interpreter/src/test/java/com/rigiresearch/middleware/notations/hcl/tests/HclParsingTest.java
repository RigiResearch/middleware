package com.rigiresearch.middleware.notations.hcl.tests;

import com.google.inject.Inject;
import com.rigiresearch.middleware.metamodels.hcl.Model;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the parser.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(HclInjectorProvider.class)
class HclParsingTest {

    /**
     * A helper for parsing HCL text.
     */
    @Inject
    private ParseHelper<Model> helper;

    @Test
    void loadModel() throws Exception {
        final Model model = this.helper.parse("Hello Xtext!");
        Assertions.assertNotNull(model);
        final EList<Resource.Diagnostic> errors = model.eResource().getErrors();
        Assertions.assertTrue(
            errors.isEmpty(),
            String.format(
                "Unexpected errors: %s",
                errors.stream()
                    .map(e -> e.toString())
                    .collect(Collectors.joining())
            )
        );
    }

}
