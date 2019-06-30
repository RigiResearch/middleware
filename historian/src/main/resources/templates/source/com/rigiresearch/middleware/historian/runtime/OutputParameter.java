package com.rigiresearch.middleware.historian.runtime;

import com.vmware.xpath.json.JsonXpath;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * An output parameter that updates the configuration every time a request is
 * executed.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class OutputParameter {

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
     * A JSON mapper.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Update this parameter based on the given content and the selector.
     * @param content The output from the request
     * @param type The content type
     * @throws IOException If there is a problem with the input stream
     */
    public void update(final String content, final String type)
        throws IOException {
        // TODO Add support for application/xml
        if (!"application/json".equals(type)) {
            OutputParameter.LOGGER.error("Unsupported content type '{}'", type);
        }
        OutputParameter.LOGGER.info(
            "Updating output parameter {}.output.parameters.{}",
            this.path,
            this.name
        );
        final JsonNode node = this.mapper.readTree(content);
        final String selector = this.config.getString(
            String.format(
                "%s.output.parameters.%s.selector",
                this.path,
                this.name
            )
        );
        this.config.setProperty(
            String.format(
                "%s.output.parameters.%s.value",
                this.path,
                this.name
            ),
            JsonXpath.find(node, selector)
                .asText()
        );
    }

}
