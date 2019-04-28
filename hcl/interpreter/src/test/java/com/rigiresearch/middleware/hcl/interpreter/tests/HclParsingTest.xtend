package com.rigiresearch.middleware.hcl.interpreter.tests

import com.google.inject.Inject
import com.rigiresearch.middleware.hcl.model.Model
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

/**
 * Tests the parser.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@ExtendWith(InjectionExtension)
@InjectWith(HclInjectorProvider)
class HclParsingTest {

    @Inject
    ParseHelper<Model> parseHelper

    @Test
    def void loadModel() {
        val result = parseHelper.parse('''
            Hello Xtext!
        ''')
        Assertions.assertNotNull(result)
        val errors = result.eResource.errors
        Assertions.assertTrue(
            errors.isEmpty,
            '''Unexpected errors: «errors.join(", ")»'''
        )
    }
}
