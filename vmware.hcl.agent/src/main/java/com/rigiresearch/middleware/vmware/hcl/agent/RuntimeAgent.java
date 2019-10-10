package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.historian.runtime.HistorianMonitor;
import com.rigiresearch.middleware.historian.runtime.UnexpectedResponseCodeException;
import com.rigiresearch.middleware.metamodels.EcorePrinter;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A run-time agent to reconcile changes in VMware vSphere back to the source
 * HCL specification.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class RuntimeAgent {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(RuntimeAgent.class);

    /**
     * A Historian monitor to detect changes in the subject vSphere.
     */
    private final HistorianMonitor monitor;

    /**
     * Default constructor.
     * @throws ConfigurationException If there is a configuration error
     *  concerning the Historian monitor
     */
    public RuntimeAgent() throws ConfigurationException {
        this.monitor = new HistorianMonitor();
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
    }

    /**
     * Stops monitoring and handling run-time changes.
     */
    public void stop() {
        this.monitor.stop();
    }

    /**
     * Handles the collected data from vSphere.
     * @param data The collected data
     */
    private void handle(final JsonNode data) {
        final Specification specification = new Data2Hcl(data).specification();
        RuntimeAgent.LOGGER.info(
            new EcorePrinter(specification).asPrettyString()
        );
    }

}
