package com.rigiresearch.middleware.historian.monitoring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
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
public final class Collector implements Cloneable {

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
     * Collects the data from the API.
     */
    public String collect() {
        String content = "";
        try {
            final CloseableHttpResponse response = this.request.response();
            content = Collector.asString(response.getEntity().getContent());
            if (response.getStatusLine().getStatusCode() == Collector.OK_CODE) {
                this.updateOutputs(content);
            } else {
                Collector.LOGGER.error(
                    "Unexpected response code '{}' from monitor '{}'.",
                    response.getStatusLine().getStatusCode(),
                    this.path
                );
            }
        } catch (final IOException exception) {
            Collector.LOGGER.error("Request execution error", exception);
        }
        return content;
    }

    /**
     * Update output parameters based on the request's response.
     * @param content The request's response
     */
    private void updateOutputs(final String content) {
        this.outputs.forEach(outputs -> {
            try {
                outputs.update(content);
            } catch (final IOException exception) {
                Collector.LOGGER.error(exception);
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
        final byte[] buffer = new byte[Collector.BUFFER_SIZE];
        int length = stream.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = stream.read(buffer);
        }
        return result.toString(StandardCharsets.UTF_8.toString());
    }

    @Override
    protected Collector clone() {
        return new Collector(
            this.path,
            this.request().clone(),
            this.outputs.stream()
                .map(Output::clone)
                .collect(Collectors.toList())
        );
    }

}
