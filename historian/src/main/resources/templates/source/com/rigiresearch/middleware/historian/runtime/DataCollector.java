package com.rigiresearch.middleware.historian.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A data collector.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class DataCollector {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The default buffer size.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * HTTP 200 status code.
     */
    private static final int OK_CODE = 200;

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * The path id associated with this collector.
     */
    @Getter
    private final String path;

    /**
     * The request this monitor performs.
     */
    @Getter
    private final Request request;

    /**
     * A list of output parameters.
     */
    private final List<Output> outputs;

    /**
     * Children data collectors.
     */
    private final List<ChildDataCollector> children;

    /**
     * Collects the data from the API.
     */
    public void collect() {
        try {
            final CloseableHttpResponse response = this.request.response();
            if (response.getStatusLine().getStatusCode() == DataCollector.OK_CODE) {
                // TODO Instantiate the schema class based on the response content
                final String content =
                    DataCollector.asString(response.getEntity().getContent());
                DataCollector.LOGGER.debug(
                    "Data collected: {}...",
                    content.substring(0, Math.min(100, content.length()))
                );
                this.updateOutputs(content);
                this.childrenCollect(content);
            } else {
                DataCollector.LOGGER.error(
                    "Unexpected response code '{}'",
                    response.getStatusLine().getStatusCode()
                );
            }
        } catch (final IOException exception) {
            DataCollector.LOGGER.error("Request execution error", exception);
        }
    }

    /**
     * Collects data from children data collectors.
     * @param content The content returned by this data collector
     * @throws IOException If something bad happens extracting the data
     */
    private void childrenCollect(final String content)
        throws IOException {
        for (final ChildDataCollector child : this.children) {
            final String variable = this.config.getString(
                String.format(
                    "%s.children.%s.input",
                    this.path,
                    child.collector().path()
                )
            );
            final String input = String.format(
                "%s.inputs.%s.value",
                child.collector().path(),
                variable
            );
            final String selector = String.format(
                "%s.inputs.%s.selector",
                child.collector().path(),
                variable
            );
            final List<String> values = new XpathValue(
                this.config.getString(selector),
                content
            ).values();
            for (final String value : values) {
                // One bye one, collect the corresponding data
                this.config.setProperty(input, value);
                child.collector().collect();
            }
        }
    }

    /**
     * Update output parameters based on the request's response.
     * @param content The request's response
     */
    private void updateOutputs(final String content) {
        this.outputs.stream()
            .forEach(outputs -> {
                try {
                    outputs.update(content);
                } catch (final IOException exception) {
                    DataCollector.LOGGER.error(exception);
                }
            });
    }

    /**
     * Returns the string representation of the given stream.
     * @param stream The input stream
     * @return A UTF-8 string
     * @throws IOException if something bad happens!
     */
    private static String asString(final InputStream stream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[DataCollector.BUFFER_SIZE];
        int length = stream.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = stream.read(buffer);
        }
        return result.toString(StandardCharsets.UTF_8.toString());
    }

    /**
     * A child data collector.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    @Accessors(fluent = true)
    @Value
    public static final class ChildDataCollector {

        /**
         * A Xpath selector for the parent collector to
         */
        private final String selector;

        /**
         * The child data collector.
         */
        private final DataCollector collector;

    }

}
