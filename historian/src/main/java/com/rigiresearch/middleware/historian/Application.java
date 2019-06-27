package com.rigiresearch.middleware.historian;

import com.rigiresearch.middleware.metamodels.AtlTransformation;
import edu.uoc.som.openapi.io.OpenAPIImporter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.m2m.atl.emftvm.Model;

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
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                "Expected an OpenAPI specification as input (in JSON format)"
            );
        }
        try {
            new Application(args[0]).start();
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
     * Create the OpenAPI model and execute the transformation.
     * @throws FileNotFoundException If the spec file does not exist
     * @throws UnsupportedEncodingException If the encoding is not supported
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private void start() throws FileNotFoundException,
        UnsupportedEncodingException, IOException {
        final edu.uoc.som.openapi.Root openstack = new OpenAPIImporter()
            .createOpenAPIModelFromJson(new File(this.spec));
        final String output = "OUT";
        final String path = "build/resources/main/openstack.monitoring.xmi";
        final Map<String, Model> result = new AtlTransformation.Builder()
            .withMetamodel(
                "monitoring",
                "../metamodels/monitoring/model-gen/Monitoring.ecore"
            )
            .withMetamodelFromJar(
                "openapi",
                "lib/edu.uoc.som.openapi-1.0.2.201804111106.jar",
                "model/openapi.ecore"
            )
            .withModel(AtlTransformation.ModelType.INPUT, "IN", openstack)
            .withModel(AtlTransformation.ModelType.OUTPUT, output, path)
            .withTransformation("src/main/resources/atl/OpenAPI2Monitoring.atl")
            .build()
            .run();
        result.get(output).getResource()
            .save(Collections.EMPTY_MAP);
    }
}
