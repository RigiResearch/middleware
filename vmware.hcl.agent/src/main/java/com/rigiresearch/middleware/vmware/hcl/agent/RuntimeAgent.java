package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.historian.runtime.HistorianMonitor;
import com.rigiresearch.middleware.historian.runtime.UnexpectedResponseCodeException;
import com.rigiresearch.middleware.metamodels.SerializationParser;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
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
    private void handle(final JsonNode data) {
        final Specification specification = new Data2Hcl(data).specification();
        try {
            final CloseableHttpClient client = HttpClients.createDefault();
            final HttpPost request = new HttpPost(
                this.config.getString("coordinator.url")
            );
            final String type = "application/xml";
            request.setHeader("Accept", type);
            request.setHeader("Content-Type", type);
            request.setEntity(new StringEntity(this.parser.asXml(specification)));
            final CloseableHttpResponse response = client.execute(request);
            final int code = response.getStatusLine().getStatusCode();
            if (code == RuntimeAgent.OKAY) {
                RuntimeAgent.LOGGER.debug(
                    "Sent current specification to the evolution coordinator"
                );
            } else {
                RuntimeAgent.LOGGER.error(
                    String.format("Unexpected response code %d", code)
                );
            }
        } catch (final IOException exception) {
            RuntimeAgent.LOGGER.error("Error serializing/sending model", exception);
        }
    }

}
