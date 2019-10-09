package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.historian.runtime.HistorianMonitor;
import com.rigiresearch.middleware.metamodels.EcorePrinter;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
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
     * A transformation from {@link JsonNode} to {@link Specification}.
     */
    private final Data2Hcl transformation;

    /**
     * Default constructor.
     * @throws ConfigurationException If there is a configuration error
     *  concerning the Historian monitor
     */
    public RuntimeAgent() throws ConfigurationException {
        this.monitor = new HistorianMonitor();
        this.transformation = new Data2Hcl();
    }

    /**
     * Starts monitoring and handling run-time changes.
     * @throws Exception If something bad happens starting the Historian monitor
     */
    public void start() throws Exception {
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
        final Specification specification =
            this.transformation.specification(data);
        RuntimeAgent.LOGGER.info(
            new EcorePrinter(specification).asPrettyString()
        );
    }

}
