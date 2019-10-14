package com.rigiresearch.middleware.historian.runtime;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
     * @param args The program arguments
     */
    public static void main(final String... args) {
        try {
            final HistorianMonitor monitor = new HistorianMonitor();
            monitor.start();
            Runtime.getRuntime()
                .addShutdownHook(new Thread(monitor::stop));
        } catch (final ConfigurationException | JAXBException | IOException
            | UnexpectedResponseCodeException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
        }
    }

}
