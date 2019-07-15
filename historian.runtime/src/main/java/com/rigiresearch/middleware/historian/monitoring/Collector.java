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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data collector.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Collector {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Collector.class);

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
     * @return The collected content
     * @throws IOException If there is a request execution error
     * @throws UnexpectedResponseCode If the response code is different than 200
     */
    public String collect() throws IOException, UnexpectedResponseCode {
        final CloseableHttpResponse response = this.request.response();
        final String content = Collector.asString(response.getEntity().getContent());
        if (response.getStatusLine().getStatusCode() == Collector.OK_CODE) {
            this.updateOutputs(content);
        } else {
            throw new UnexpectedResponseCode(
                "Unexpected response code '%s' from monitor '%s'.",
                response.getStatusLine().getStatusCode(),
                this.path
            );
        }
        return content;
    }

    /**
     * Update output parameters based on the request's response.
     * @param content The request's response
     */
    private void updateOutputs(final String content) {
        this.outputs.forEach(output -> {
            try {
                output.update(content);
            } catch (final IOException exception) {
                Collector.LOGGER.error("Error updating output", exception);
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

    /**
     * Duplicates this object.
     * @return A clone
     */
    Collector duplicate() {
        return new Collector(
            this.path,
            this.request().duplicate(),
            this.outputs.stream()
                .map(Output::duplicate)
                .collect(Collectors.toList())
        );
    }

}
