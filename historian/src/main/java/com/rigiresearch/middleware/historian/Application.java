package com.rigiresearch.middleware.historian;

import com.rigiresearch.middleware.historian.templates.MonitoringTemplate;
import com.rigiresearch.middleware.metamodels.AtlTransformation;
import com.rigiresearch.middleware.metamodels.monitoring.MonitoringPackage;
import com.rigiresearch.middleware.metamodels.monitoring.Root;
import edu.uoc.som.openapi.OpenAPIPackage;
import edu.uoc.som.openapi.io.OpenAPIImporter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main class.
 * TODO use jcommander to handle the parameters.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Error code for file-not-found exception.
     */
    private static final int ERROR_NTE = 1;

    /**
     * Error code for unsupported-encoding exception.
     */
    private static final int ERROR_EE = 2;

    /**
     * Error code for input/output exception.
     */
    private static final int ERROR_IO = 3;

    /**
     * The path to an OpenAPI spec.
     */
    private final String spec;

    /**
     * The path to a directory where the monitoring code will be generated.
     */
    private final String target;

    /**
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Expected a JSON OpenAPI specification and target directory as input"
            );
        }
        try {
            new Application(args[0], args[1]).start();
        } catch (final FileNotFoundException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
            System.exit(Application.ERROR_NTE);
        } catch (final UnsupportedEncodingException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
            System.exit(Application.ERROR_EE);
        } catch (final IOException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
            System.exit(Application.ERROR_IO);
        }
    }

    /**
     * Generates a gradle project containing the monitoring code for a
     * particular API specification.
     * @throws FileNotFoundException If the spec file does not exist
     * @throws UnsupportedEncodingException If the encoding is not supported
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private void start() throws FileNotFoundException,
        UnsupportedEncodingException, IOException {
        final edu.uoc.som.openapi.Root root = new OpenAPIImporter()
            .createOpenAPIModelFromJson(new File(this.spec));
        final String output = "OUT";
        new MonitoringTemplate().generateFiles(
            (Root) new AtlTransformation.Builder()
                .withMetamodel(MonitoringPackage.eINSTANCE)
                .withMetamodel(OpenAPIPackage.eINSTANCE)
                .withModel(AtlTransformation.ModelType.INPUT, "IN", root)
                .withModel(AtlTransformation.ModelType.OUTPUT, output, "monitoring.xmi")
                .withTransformation("src/main/resources/atl/OpenAPI2Monitoring.atl")
                .build()
                .run()
                .get(output)
                .getResource()
                .getContents()
                .get(0),
            new File(this.target)
        );
    }
}
