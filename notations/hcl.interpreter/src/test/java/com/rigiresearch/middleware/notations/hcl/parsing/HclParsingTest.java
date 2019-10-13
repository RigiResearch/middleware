package com.rigiresearch.middleware.notations.hcl.parsing;

import com.google.inject.Inject;
import com.rigiresearch.middleware.metamodels.hcl.Dictionary;
import com.rigiresearch.middleware.metamodels.hcl.NameValuePair;
import com.rigiresearch.middleware.metamodels.hcl.Resource;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.HclInjectorProvider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.EList;
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
    private ParseHelper<Specification> helper;

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    void testSimpleModel() throws Exception {
        final String source = "##############################################################\n"
            + "# Define pattern variables \n"
            + "##############################################################\n"
            + "\n"
            + "##############################################################\n"
            + "# Vsphere data for provider\n"
            + "##############################################################\n"
            + "data \"vsphere_datacenter\" \"vm_1_datacenter\" {\n"
            + "  name = \"${var.vm_1_datacenter}\"\n"
            + "}";
        final Specification specification = this.helper.parse(source);
        Assertions.assertNotNull(specification);
        final EList<org.eclipse.emf.ecore.resource.Resource.Diagnostic> errors =
            specification.eResource().getErrors();
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

    @Test
    void testComments() throws Exception {
        final List<String> lines = Arrays.asList(
            "# A first comment",
            "# A second comment"
        );
        final String comments = String.format("%s\n", String.join("\n", lines));
        final String source = comments
            + "variable \"node_count\" {\n"
            + comments
            + "\tdefault = 1\n"
            + "}";
        final Specification specification = this.helper.parse(source);
        final Resource variable = specification.getResources().get(0);
        final NameValuePair attribute = ((Dictionary) variable.getValue())
            .getElements()
            .get(0);
        Assertions.assertNotNull(
            specification,
            "The specification couldn't be parsed"
        );
        Assertions.assertEquals(
            lines.size(),
            variable.getComment().getLines().size(),
            "Unexpected number of resource comments"
        );
        Assertions.assertEquals(
            lines.size(),
            attribute.getComment().getLines().size(),
            "Unexpected number of attribute comments"
        );
    }

}
