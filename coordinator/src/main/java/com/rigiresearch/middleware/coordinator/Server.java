package com.rigiresearch.middleware.coordinator;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

/**
 * An HTTP server to handle evolution requests.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Server {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Server.class);

    /**
     * An evolution coordinator.
     */
    private final EvolutionCoordination coordinator;

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * Default constructor.
     * @throws ConfigurationException If the configuration file is not found
     */
    public Server()
        throws ConfigurationException {
        this.coordinator = new EvolutionCoordination();
        this.config = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
            PropertiesConfiguration.class
        ).configure(
            new Parameters().properties()
                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                .setFileName("coordinator.properties")
        ).getConfiguration();
    }

    /**
     * Runs an HTTP server for handling evolution requests.
     */
    public void run() {
        Server.LOGGER.info("Starting server");
        Spark.port(this.config.getInt("coordinator.port"));
        final int okay = 200;
        Spark.post("/", (request, response) -> {
            this.coordinator.toString();
            Server.LOGGER.info(request.body());
            response.status(okay);
            return "";
        });
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        Server.LOGGER.info("Stopping server");
        Spark.stop();
    }

}
