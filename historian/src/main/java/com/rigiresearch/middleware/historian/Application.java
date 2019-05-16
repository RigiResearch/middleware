package com.rigiresearch.middleware.historian;

import edu.uoc.som.openapi.Root;
import edu.uoc.som.openapi.io.OpenAPIImporter;
import java.io.File;
import java.io.FileNotFoundException;
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
     * The path to an OpenAPI spec.
     */
    private final String spec;

    /**
     * The path to the output ecore file.
     */
    private final String metamodel;

    /**
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Expected an OpenAPI specification as input"
            );
        }
        try {
            new Application(args[0], args[1]).start();
        } catch (final FileNotFoundException exception) {
            Application.LOGGER.error(exception.getMessage());
            System.exit(Application.ERROR_NTE);
        } catch (final UnsupportedEncodingException exception) {
            Application.LOGGER.error(exception.getMessage());
            System.exit(Application.ERROR_EE);
        }
    }

    /**
     * Create the OpenAPI model and execute the transformation.
     * @throws FileNotFoundException If the spec file does not exist
     * @throws UnsupportedEncodingException If the encoding is not supported
     */
    private void start()
        throws FileNotFoundException, UnsupportedEncodingException {
        final Root model = new OpenAPIImporter()
            .createOpenAPIModelFromJson(new File(this.spec));
        model.getApi();
    }

}
