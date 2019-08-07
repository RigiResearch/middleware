package com.rigiresearch.middleware.historian.monitoring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data collector.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class Request {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Request.class);

    /**
     * The default buffer size.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * HTTP 200 status code.
     */
    private static final int OK_CODE = 200;

    /**
     * The request inputs.
     */
    private final List<Input> inputs;

    /**
     * The url from which this collector collects the data.
     */
    private final URL url;

    /**
     * Collects data from the associated resource.
     * TODO Support basic authentication for the login monitor.
     * @return The HTTP response
     * @throws IOException If there is an issue executing the HTTP request
     */
    public CloseableHttpResponse response() throws IOException {
        final URI uri = this.uri(this.url);
        final CloseableHttpClient client = HttpClients.createDefault();
        final CloseableHttpResponse response;
        final HttpUriRequest request = new HttpGet(uri);
        this.parameters(Input.Location.HEADER)
            .forEach(p -> request.addHeader(p.getName(), p.getValue()));
        response = client.execute(request);
        return response;
    }

    /**
     * Collects the data from the associated URL.
     * @return The collected content
     * @throws IOException If there is a request execution error
     * @throws UnexpectedResponseCodeException If the response code is different than 200
     */
    public String data() throws IOException, UnexpectedResponseCodeException {
        final CloseableHttpResponse response = this.response();
        if (response.getStatusLine().getStatusCode() != Request.OK_CODE) {
            throw new UnexpectedResponseCodeException(
                "Unexpected response code '%s' from URL '%s'.",
                response.getStatusLine().getStatusCode(),
                this.url
            );
        }
        return this.asString(response.getEntity().getContent());
    }

    /**
     * Builds the target URI replacing the parameters where corresponds.
     * @param initial The initial URL
     * @return A URI with the corresponding parameters set
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private URI uri(final URL initial) {
        final List<String> flatpath = Arrays.asList(initial.getPath());
        this.parameters(Input.Location.PATH)
            .forEach(input -> {
                flatpath.set(
                    0,
                    flatpath.get(0).replaceAll(
                        String.format("\\{%s\\}", input.getName()),
                        input.getValue()
                    )
                );
            });
        final URIBuilder builder = new URIBuilder()
            .setScheme(initial.getProtocol())
            .setHost(initial.getHost())
            .setPath(flatpath.get(0));
        this.parameters(
            Input.Location.QUERY,
            Input.Location.FORM_DATA
        ).forEach(param -> {
            builder.setParameter(param.getName(), param.getValue());
        });
        try {
            return builder.build();
        } catch (final URISyntaxException exception) {
            Request.LOGGER.error("Malformed URI", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Filters out the inputs based on the given parameter locations.
     * @param criteria The filtering criteria
     * @return A stream of parameters
     */
    private Stream<Input> parameters(final Input.Location... criteria) {
        return this.inputs.stream()
            .filter(i -> Arrays.asList(criteria).contains(i.getLocation()));
    }

    /**
     * Returns the string representation of the given stream.
     * @param stream The input stream
     * @return A UTF-8 string
     * @throws IOException if something bad happens!
     */
    private static String asString(final InputStream stream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[Request.BUFFER_SIZE];
        int length = stream.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = stream.read(buffer);
        }
        return result.toString(StandardCharsets.UTF_8.toString());
    }

}
