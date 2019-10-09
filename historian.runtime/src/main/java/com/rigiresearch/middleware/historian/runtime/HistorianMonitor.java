package com.rigiresearch.middleware.historian.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.GraphParser;
import com.rigiresearch.middleware.historian.runtime.graph.Monitor;
import it.sauronsoftware.cron4j.Scheduler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.bind.JAXBException;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A run-time monitor to collect data from a target API.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public final class HistorianMonitor {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(HistorianMonitor.class);

    /**
     * A JSON object mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A list of consumers for reporting run-time changes.
     */
    private final List<Consumer<JsonNode>> consumers;

    /**
     * The configuration generated by Historian.
     */
    private final Configuration config;

    /**
     * The task scheduler.
     */
    private final Scheduler scheduler;

    /**
     * The current JSON object.
     */
    private JsonNode previous;

    /**
     * Default constructor.
     * @throws ConfigurationException If there is an error building the
     *  configuration
     */
    public HistorianMonitor() throws ConfigurationException {
        this.consumers = new ArrayList<>(1);
        this.config = HistorianMonitor.initialize();
        this.scheduler = new Scheduler();
        this.previous = HistorianMonitor.MAPPER.createObjectNode();
    }

    /**
     * Subscribes a consumer to listen for run-time changes.
     * @param consumer The consumer
     * @return Whether the consumer was subscribed.
     */
    public boolean subscribe(final Consumer<JsonNode> consumer) {
        return this.consumers.add(consumer);
    }

    /**
     * Loads the configuration file.
     * @return A {@link Configuration} instance.
     * @throws ConfigurationException If there is an error building the
     *  configuration
     */
    private static Configuration initialize() throws ConfigurationException {
        final Parameters params = new Parameters();
        final FileBasedConfigurationBuilder<FileBasedConfiguration> properties =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class
            ).configure(
                params.properties()
                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                    .setFileName("default.properties")
            );
        final FileBasedConfigurationBuilder<FileBasedConfiguration> custom =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class
            ).configure(
                params.properties()
                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                    .setFileName("custom.properties")
            );
        final CompositeConfiguration composite = new CompositeConfiguration();
        composite.addConfiguration(custom.getConfiguration());
        composite.addConfiguration(properties.getConfiguration());
        return composite;
    }

    /**
     * Start the monitors.
     * @throws JAXBException If anything fails reading the configuration graph
     * @throws IOException If the authentication URL is malformed
     * @throws UnexpectedResponseCodeException If there is an authentication problem
     */
    public void start() throws JAXBException, IOException,
        UnexpectedResponseCodeException {
        final ForkAndCollectAlgorithm algorithm =
            new ForkAndCollectAlgorithm(
                new GraphParser()
                    .withBindings("bindings.xml")
                    .instance(
                        new File(
                            Thread.currentThread()
                                .getContextClassLoader()
                                .getResource("configuration.xml")
                                .getFile()
                        )
                    ),
                this.config
            );
        this.setupAuthProviders(algorithm.getGraph());
        this.scheduler.schedule(
            this.config.getString("periodicity"), () -> this.collect(algorithm)
        );
        this.scheduler.start();
    }

    /**
     * Stops the scheduler, thus stopping any scheduled monitor/request.
     */
    public void stop() {
        this.scheduler.stop();
    }

    /**
     * Creates the authentication providers.
     * @param graph The configuration graph
     * @throws IOException See {@link ApiKeyProvider#setup()}
     * @throws UnexpectedResponseCodeException See {@link ApiKeyProvider#setup()}
     */
    private void setupAuthProviders(final Graph<Monitor> graph)
        throws IOException, UnexpectedResponseCodeException {
        final String[] methods = this.config.getStringArray("auth");
        for (final String method : methods) {
            final String type =
                this.config.getString(String.format("auth.%s.type", method));
            if ("key".equals(type)) {
                new ApiKeyProvider(method, this.config, graph, this.scheduler)
                    .setup();
            } else {
                throw new UnsupportedOperationException(
                    String.format(
                        "Authentication method \"%s\" is not supported yet",
                        type
                    )
                );
            }
        }
    }

    /**
     * Collects the data from the remote server.
     * @param algorithm An instance of the Fork and Collect algorithm
     */
    private void collect(final ForkAndCollectAlgorithm algorithm) {
        try {
            final JsonNode result = algorithm.data();
            if (result.equals(this.previous)) {
                HistorianMonitor.LOGGER.info("The monitored resources have not changed");
            } else {
                this.previous = result;
                HistorianMonitor.LOGGER.info(
                    "{}",
                    HistorianMonitor.MAPPER.writeValueAsString(this.previous)
                );
            }
        } catch (final UnexpectedResponseCodeException | IOException
            | com.rigiresearch.middleware.historian.runtime.ConfigurationException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
