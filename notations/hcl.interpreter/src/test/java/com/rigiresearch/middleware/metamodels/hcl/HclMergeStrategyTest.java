package com.rigiresearch.middleware.metamodels.hcl;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
     * Format string for the previous file.
     */
    private static final String PREVIOUS = "%s/previous.tf";

    /**
     * Format string for the current file.
     */
    private static final String CURRENT = "%s/current.tf";

    /**
     * Format string for the result file.
     */
    private static final String RESULT = "%s/result.tf";

    /**
     * An HCL parser.
     */
    private static final HclParser PARSER = new HclParser();

    @CsvSource({
        "empty",
        // "existing-attribute",
        // "existing-comment-removed",
        // "existing-comment-updated",
        // "existing-resource-not-updated",
        "non-existing-attribute",
        "non-existing-comment",
        "non-existing-resource"
        // "various"
    })
    @ParameterizedTest
    void testMerge(final String directory) throws Exception {
        final Specification previous = HclMergeStrategyTest.PARSER.parse(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.PREVIOUS, directory)
            )
        );
        final Specification current = HclMergeStrategyTest.PARSER.parse(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.CURRENT, directory)
            )
        );
        final Specification result = new HclMergeStrategy()
            .merge(previous, current);
        Assertions.assertEquals(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.RESULT, directory)
            ),
            HclMergeStrategyTest.PARSER.parse(result),
            HclMergeStrategyTest.ERROR_MESSAGE
        );
    }

    @Disabled
    @CsvSource("complex")
    @ParameterizedTest
    void testSpecificationSet(final String directory) throws Exception {
        final Specification previous = HclMergeStrategyTest.PARSER.parse(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.PREVIOUS, directory)
            )
        );
        final SpecificationSet set = new SpecificationSet(previous);
        final Specification current = HclMergeStrategyTest.PARSER.parse(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.CURRENT, directory)
            )
        );
        final Specification result = new HclMergeStrategy()
            .merge(set.unified(), current);
        Assertions.assertEquals(
            HclMergeStrategyTest.source(
                String.format(HclMergeStrategyTest.RESULT, directory)
            ),
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
    private static String source(final String path) throws Exception {
        final byte[] buffer = new byte[HclMergeStrategyTest.DEFAULT_SIZE];
        try (InputStream input = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(path)) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            int length = input.read(buffer);
            while (length > 0) {
                output.write(buffer, 0, length);
                length = input.read(buffer);
            }
            output.close();
            return output.toString();
        }
    }

}
