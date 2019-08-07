package com.rigiresearch.middleware.historian.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rigiresearch.middleware.graph.GraphParser;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests {@link ForkAndCollectAlgorithm}.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@Testcontainers
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public final class AlgorithmTest {

    /**
     * Formatter string.
     */
    private static final String FORMAT = "%s/%s";

    /**
     * A mock server that runs on a Docker container.
     */
    @Container
    @SuppressWarnings("PMD.UnusedPrivateField")
    private final GenericContainer container = new ApiMockServer().setup();

    @CsvSource({"empty", "nodeps", "simple"})
    @ParameterizedTest
    void testConfiguration(final String path) throws Exception {
        final Parameters params = new Parameters();
        final Configuration config =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class
            ).configure(
                params.properties()
                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                    .setFileName(
                        String.format(
                            AlgorithmTest.FORMAT,
                            path,
                            "default.properties"
                        )
                    )
            ).getConfiguration();
        final ForkAndCollectAlgorithm algorithm = new ForkAndCollectAlgorithm(
            new GraphParser()
                .instance(
                    new File(
                        Thread.currentThread()
                            .getContextClassLoader()
                            .getResource(
                                String.format(
                                    AlgorithmTest.FORMAT,
                                    path,
                                    "graph.xml"
                                )
                            )
                            .getFile()
                    )
                ),
            config
        );
        final ObjectMapper mapper = new ObjectMapper();
        Assertions.assertEquals(
            mapper.readTree(this.resourceContent(path, "output.json")),
            algorithm.data(),
            "Incorrect output"
        );
    }

    /**
     * Reads the content of a resource.
     * @param path The file path
     * @param filename The name of the resource
     * @return The content of the specified file
     * @throws IOException If something bad happens reading the file
     * @throws URISyntaxException If something bad happens finding the file
     */
    private String resourceContent(final String path, final String filename)
        throws IOException, URISyntaxException {
        final ClassLoader loader = Thread.currentThread()
            .getContextClassLoader();
        return new String(
            Files.readAllBytes(
                Paths.get(
                    loader.getResource(
                        String.format(
                            AlgorithmTest.FORMAT,
                            path,
                            filename
                        )
                    ).toURI()
                )
            ),
            Charset.forName("UTF-8")
        );
    }

}
