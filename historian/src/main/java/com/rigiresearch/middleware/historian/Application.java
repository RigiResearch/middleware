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
     * The path to an OpenAPI specification.
     */
    private final String path;

    /**
     * Create the OpenAPI model and execute the transformation.
     * @throws FileNotFoundException If the specification file does not exist
     * @throws UnsupportedEncodingException If the encoding is not supported
     */
    private void start()
        throws FileNotFoundException, UnsupportedEncodingException {
        final Root model = new OpenAPIImporter()
            .createOpenAPIModelFromJson(new File(this.path));
        model.getApi();
    }

    /**
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                "Expected an OpenAPI specification as input"
            );
        }
        try {
            new Application(args[0]).start();
        } catch (final FileNotFoundException exception) {
            Application.LOGGER.error(exception.getMessage());
            System.exit(1);
        } catch (final UnsupportedEncodingException exception) {
            Application.LOGGER.error(exception.getMessage());
            System.exit(2);
        }
    }

}
