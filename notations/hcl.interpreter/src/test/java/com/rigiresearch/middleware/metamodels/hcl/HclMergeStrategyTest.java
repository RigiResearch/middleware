package com.rigiresearch.middleware.metamodels.hcl;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link HclMergeStrategy}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Tag("integration")
final class HclMergeStrategyTest {

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
    private static final HclParser PARSER = new HclParser();

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
        final Specification previous = HclMergeStrategyTest.PARSER.parse(
            this.source(String.format("%s/previous.tf", directory))
        );
        final Specification current = HclMergeStrategyTest.PARSER.parse(
            this.source(String.format("%s/current.tf", directory))
        );
        final Specification result = new HclMergeStrategy()
            .merge(previous, current);
        Assertions.assertEquals(
            this.source(String.format("%s/result.tf", directory)),
            HclMergeStrategyTest.PARSER.parse(result),
            HclMergeStrategyTest.ERROR_MESSAGE
        );
    }

    /**
     * Reads a resource file from its path.
     * @param path The file path
     * @return A non-null string
     * @throws Exception If there's an I/O error
     */
    private String source(final String path) throws Exception {
        final byte[] buffer = new byte[HclMergeStrategyTest.DEFAULT_SIZE];
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
