package com.rigiresearch.middleware.coordinator;

import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link EvolutionCoordination}.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@Tag("integration")
final class EvolutionCoordinationTest {

    /**
     * Default buffer size.
     */
    private static final int DEFAULT_SIZE = 8 * 1024;

    /**
     * The default error message.
     */
    private static final String ERROR_MESSAGE = "Incorrect merge result";

    /**
     * An HCL parser.
     */
    private final HclParser parser;

    /**
     * Default constructor.
     */
    EvolutionCoordinationTest() {
        this.parser = new HclParser();
    }

    @CsvSource({
        "empty",
        "existing-attribute",
        "existing-comment-removed",
        "existing-comment-updated",
        "non-existing-attribute",
        "non-existing-comment",
        "non-existing-resource",
        "various"
    })
    @ParameterizedTest
    void testMerge(final String directory) throws Exception {
        final Specification previous =
            this.specification(String.format("%s/previous.tf", directory));
        final Specification current =
            this.specification(String.format("%s/current.tf", directory));
        final Specification result = new EvolutionCoordination()
            .merge(previous, current);
        Assertions.assertEquals(
            this.source(String.format("%s/result.tf", directory)),
            this.parser.parse(result),
            EvolutionCoordinationTest.ERROR_MESSAGE
        );
    }

    /**
     * Instantiates a {@link Specification} from a file path.
     * @param path The file path
     * @return A non-null specification
     * @throws Exception If there's an I/O error
     */
    private Specification specification(final String path) throws Exception {
        return this.parser.parse(this.source(path));
    }

    /**
     * Reads a resource file from its path.
     * @param path The file path
     * @return A non-null string
     * @throws Exception If there's an I/O error
     */
    private String source(final String path) throws Exception {
        final byte[] buffer = new byte[EvolutionCoordinationTest.DEFAULT_SIZE];
        final InputStream input = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(path);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        int length = input.read(buffer);
        while (length > 0) {
            output.write(buffer, 0, length);
            length = input.read(buffer);
        }
        input.close();
        output.close();
        return output.toString();
    }

}
