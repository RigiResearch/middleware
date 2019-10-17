package com.rigiresearch.middleware.coordinator;

import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.eclipse.jgit.api.errors.GitAPIException;
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
     * The default port in case none is provided.
     */
    private static final int PORT = 5050;

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
     * @throws GitAPIException See {@link EvolutionCoordination}
     * @throws IOException See {@link EvolutionCoordination}
     * @throws URISyntaxException See {@link EvolutionCoordination}
     */
    public Server() throws ConfigurationException, GitAPIException, IOException,
        URISyntaxException {
        this.config = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
            PropertiesConfiguration.class
        ).configure(
            new Parameters().properties()
                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                .setFileName("coordinator.properties")
        ).getConfiguration();
        this.coordinator = new EvolutionCoordination(this.config);
    }

    /**
     * Runs an HTTP server for handling evolution requests.
     */
    public void run() {
        Spark.port(this.config.getInt("coordinator.port", Server.PORT));
        final int okay = 200;
        Spark.post("/", (request, response) -> {
            this.coordinator.runtimeUpdate(request.body());
            response.status(okay);
            // TODO return a summary of what was done
            return "";
        });
        Server.LOGGER.info(
            String.format(
                "Started the evolution coordinator's server (port %d)",
                Spark.port()
            )
        );
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        Spark.stop();
        Server.LOGGER.info("Stopped the evolution coordinator's server");
    }

}
