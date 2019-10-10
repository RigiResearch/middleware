package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link Data2Hcl}.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
class Data2HclTest {

    @CsvSource("input1.json")
    @ParameterizedTest
    void testTransformation(final String path) throws IOException {
        final JsonNode data = new ObjectMapper().readTree(
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)
        );
        final Data2Hcl transformation = new Data2Hcl(data);
        final Specification specification = transformation.specification();
        Assertions.assertNotNull(specification);
    }

}
