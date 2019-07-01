package com.rigiresearch.middleware.historian;

import com.rigiresearch.middleware.historian.monitoring.MonitoringConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOGGER = LogManager.getLogger();

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
        final MonitoringConfiguration config = new MonitoringConfiguration();
        try {
            config.startMonitoring();
        } catch (final InterruptedException exception) {
            Application.LOGGER.error("Error starting the monitors", exception);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                config.stopMonitoring();
            }
        });
    }
}
