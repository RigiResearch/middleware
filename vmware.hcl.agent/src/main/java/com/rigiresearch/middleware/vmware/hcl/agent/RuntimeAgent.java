package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.historian.runtime.HistorianMonitor;
import com.rigiresearch.middleware.historian.runtime.UnexpectedResponseCodeException;
import com.rigiresearch.middleware.metamodels.SerializationParser;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A run-time agent to reconcile changes in VMware vSphere back to the source
 * HCL specification.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public final class RuntimeAgent {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(RuntimeAgent.class);

    /**
     * HTTP 200 response code.
     */
    private static final int OKAY = 200;

    /**
     * Initial capacity for maps/collections.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * A Historian monitor to detect changes in the subject vSphere.
     */
    private final HistorianMonitor monitor;

    /**
     * A parser to serialize Ecore models.
     */
    private final SerializationParser parser;

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * Previous template values.
     */
    private Map<String, String> previous;

    /**
     * Default constructor.
     * @throws ConfigurationException If there is a configuration error
     *  concerning the Historian monitor
     */
    public RuntimeAgent() throws ConfigurationException {
        this.monitor = new HistorianMonitor();
        this.parser = new SerializationParser();
        this.config = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
            PropertiesConfiguration.class
        ).configure(
            new Parameters().properties()
                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                .setFileName("agent.properties")
        ).getConfiguration();
        this.previous = new HashMap<>(RuntimeAgent.INITIAL_CAPACITY);
    }

    /**
     * Starts monitoring and handling run-time changes.
     * @throws JAXBException If there is a marshalling error
     * @throws IOException If something bad happens in the monitor
     * @throws UnexpectedResponseCodeException If there is an unexpected
     *  response while collecting the data
     */
    public void start()
        throws JAXBException, IOException, UnexpectedResponseCodeException {
        this.monitor.subscribe(this::handle);
        this.monitor.start();
        RuntimeAgent.LOGGER.info("Started the Historian monitor");
    }

    /**
     * Stops monitoring and handling run-time changes.
     */
    public void stop() {
        this.monitor.stop();
        RuntimeAgent.LOGGER.info("Stopped the Historian monitor");
    }

    /**
     * Handles the collected data from vSphere.
     * @param data The collected data
     */
    public void handle(final JsonNode data) {
        final Data2Hcl transformation = new Data2Hcl(data);
        final Specification specification = transformation.specification();
        try {
            final CloseableHttpResponse response = RuntimeAgent.postRequest(
                this.config.getString("coordinator.url"),
                "application/xml",
                this.parser.asXml(specification)
            );
            final int code = response.getStatusLine().getStatusCode();
            if (code == RuntimeAgent.OKAY) {
                RuntimeAgent.LOGGER.info(
                    "Sent current specification to the evolution coordinator"
                );
            } else {
                RuntimeAgent.LOGGER.error(
                    String.format("Unexpected response code %d", code)
                );
            }
            this.logValueReport(transformation.variableValues());
        } catch (final IOException exception) {
            RuntimeAgent.LOGGER.error("Error serializing/sending model", exception);
        }
    }

    /**
     * Reports added, changed and removed values.
     * @param current The current values
     */
    private void logValueReport(final Map<String, String> current) {
        synchronized (RuntimeAgent.LOGGER) {
            RuntimeAgent.LOGGER.info("The current status of the template is as follows");
            if (this.previous.equals(current)) {
                RuntimeAgent.LOGGER.info(
                    "The collected values have not changed"
                );
            } else {
                // Bold colors
                final String green = "\033[1;32m";
                final String red = "\033[1;31m";
                final String format = "{}{} = {}{}";
                for (final Map.Entry<String, String> entry : current.entrySet()) {
                    final boolean exists = this.previous.containsKey(entry.getKey());
                    final String property = entry.getKey();
                    final String value = entry.getValue();
                    if (exists && this.previous.get(property).equals(value)) {
                        // Didn't change
                        RuntimeAgent.LOGGER.info("{} = {}", property, value);
                        this.previous.remove(entry.getKey());
                    } else if (exists) {
                        // Changed
                        RuntimeAgent.LOGGER.info(
                            "{} = {}{}{} -> {}{}{}",
                            property,
                            red,
                            this.previous.get(property),
                            red,
                            green,
                            value,
                            green
                        );
                        this.previous.remove(entry.getKey());
                    } else {
                        // Added
                        RuntimeAgent.LOGGER.info(
                            format,
                            green,
                            property,
                            value,
                            green
                        );
                    }
                }
                if (!this.previous.isEmpty()) {
                    for (final Map.Entry<String, String> entry
                        : this.previous.entrySet()) {
                        // removed
                        RuntimeAgent.LOGGER.info(
                            format,
                            red,
                            entry.getKey(),
                            entry.getValue(),
                            red
                        );
                    }
                }
                this.previous = current;
            }
        }
    }

    /**
     * Makes a POST request to a certain URL.
     * @param url The target URL
     * @param type The type of content being sent
     * @param body The content
     * @return The request's response
     * @throws IOException If there's an I/O error
     */
    private static CloseableHttpResponse postRequest(final String url,
        final String type, final String body) throws IOException {
        final CloseableHttpClient client = HttpClients.createDefault();
        final HttpPost request = new HttpPost(url);
        request.setHeader("Accept", type);
        request.setHeader("Content-Type", type);
        request.setEntity(new StringEntity(body));
        return client.execute(request);
    }

}
