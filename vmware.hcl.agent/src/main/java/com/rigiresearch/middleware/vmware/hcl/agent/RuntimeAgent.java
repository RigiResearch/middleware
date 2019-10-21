package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rigiresearch.middleware.historian.runtime.HistorianMonitor;
import com.rigiresearch.middleware.historian.runtime.UnexpectedResponseCodeException;
import com.rigiresearch.middleware.metamodels.SerializationParser;
import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
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
import org.eclipse.emf.ecore.util.EcoreUtil;
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
     * A JSON mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    private Map<String, String> values;

    /**
     * Previous specification.
     */
    private Specification specification;

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
        this.values = new HashMap<>(RuntimeAgent.INITIAL_CAPACITY);
        this.specification = HclFactory.eINSTANCE.createSpecification();
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
        final Specification current = transformation.specification();
        final Map<String, String> map = transformation.variableValues();
        try {
            final ObjectNode body = RuntimeAgent.MAPPER.createObjectNode();
            boolean changed = false;
            if (EcoreUtil.equals(this.specification, current)) {
                RuntimeAgent.LOGGER.info("The specification did not changed");
            } else {
                body.set(
                    "specification",
                    RuntimeAgent.MAPPER.valueToTree(this.parser.asXml(current))
                );
                changed = true;
            }
            if (!this.values.equals(map)) {
                body.set("values", RuntimeAgent.MAPPER.valueToTree(map));
                changed = true;
            }
            if (changed) {
                this.postRequest(RuntimeAgent.MAPPER.writeValueAsString(body));
            }
        } catch (final IOException exception) {
            RuntimeAgent.LOGGER.error("Error serializing/sending model", exception);
        } finally {
            this.specification = current;
        }
        this.logValueReport(transformation.variableValues());
    }

    /**
     * Reports added, changed and removed values.
     * @param current The current values
     */
    private void logValueReport(final Map<String, String> current) {
        synchronized (RuntimeAgent.LOGGER) {
            if (this.values.equals(current)) {
                RuntimeAgent.LOGGER.info(
                    "The specification values did not changed"
                );
            } else {
                RuntimeAgent.LOGGER.info("The specification values are as follows");
                // Bold colors
                final String green = "\033[1;32m";
                final String red = "\033[1;31m";
                final String format = "{}{} = {}{}";
                for (final Map.Entry<String, String> entry : current.entrySet()) {
                    final boolean exists = this.values.containsKey(entry.getKey());
                    final String property = entry.getKey();
                    final String value = entry.getValue();
                    if (exists && this.values.get(property).equals(value)) {
                        // Didn't change
                        RuntimeAgent.LOGGER.info("{} = {}", property, value);
                        this.values.remove(entry.getKey());
                    } else if (exists) {
                        // Changed
                        RuntimeAgent.LOGGER.info(
                            "{} = {}{}{} -> {}{}{}",
                            property,
                            red,
                            this.values.get(property),
                            red,
                            green,
                            value,
                            green
                        );
                        this.values.remove(entry.getKey());
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
                if (!this.values.isEmpty()) {
                    for (final Map.Entry<String, String> entry
                        : this.values.entrySet()) {
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
                this.values = current;
            }
        }
    }

    /**
     * Makes a POST request to the evolution coordinator.
     * @param body The content
     * @throws IOException If there's an I/O error
     */
    private void postRequest(final String body) throws IOException {
        final CloseableHttpClient client = HttpClients.createDefault();
        final HttpPost request = new HttpPost(this.config.getString("coordinator.url"));
        final String type = "application/json";
        request.setHeader("Accept", type);
        request.setHeader("Content-Type", type);
        request.setEntity(new StringEntity(body));
        final CloseableHttpResponse response = client.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        if (code == RuntimeAgent.OKAY) {
            RuntimeAgent.LOGGER.info(
                "Sent request to the evolution coordinator"
            );
        } else {
            RuntimeAgent.LOGGER.error(
                String.format("Unexpected response code %d", code)
            );
        }
    }

}
