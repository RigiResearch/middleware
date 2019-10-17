package com.rigiresearch.middleware.coordinator;

import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Application.class);

    /**
     * Default constructor.
     */
    private Application() {
    }

    /**
     * The main entry point.
     * @param args The application arguments
     */
    public static void main(final String... args) {
        try {
            final Server server = new Server();
            server.run();
            Runtime.getRuntime()
                .addShutdownHook(new Thread(server::stop));
        } catch (final ConfigurationException | GitAPIException | IOException
            | URISyntaxException exception) {
            Application.LOGGER.error("Error configuring the server", exception);
        }
    }

}
