package com.rigiresearch.middleware.historian.monitoring;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An output parameter that updates the configuration every time a request is
 * executed.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class Output implements Cloneable {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The properties configuration;
     */
    private final Configuration config;

    /**
     * The path to which this parameter is associated.
     */
    private final String path;

    /**
     * The name of this output parameter.
     */
    private final String name;

    /**
     * Update this parameter based on the given content and the selector.
     * @param content The output from the request
     * @throws IOException If there is a problem with the input stream
     */
    public void update(final String content)
        throws IOException {
        Output.LOGGER.debug(
            "Updating output {}.outputs.{}",
            this.path,
            this.name
        );
        final String selector = this.config.getString(
            String.format("%s.outputs.%s.selector", this.path, this.name)
        );
        this.config.setProperty(
            String.format("%s.outputs.%s.value", this.path, this.name),
            new XpathValue(selector, content).value()
        );
    }

    @Override
    protected Output clone() {
        return new Output(
            this.config,
            this.path,
            this.name
        );
    }

}
