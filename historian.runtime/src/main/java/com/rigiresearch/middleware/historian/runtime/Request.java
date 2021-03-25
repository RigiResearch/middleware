package com.rigiresearch.middleware.historian.runtime;

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
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data collector.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
@RequiredArgsConstructor
public final class Request {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Request.class);

    /**
     * An HTTP client.
     */
    private static final CloseableHttpClient CLIENT =
        HttpClients.createDefault();

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
     * A credentials provider (optional).
     */
    private CredentialsProvider provider;

    /**
     * Sets a username and password to use basic authentication.
     * @param username The username
     * @param password The password
     * @return This request
     */
    public Request withCredentials(final String username, final String password) {
        this.provider = new BasicCredentialsProvider();
        this.provider.setCredentials(
            new AuthScope(this.url.getHost(), this.url.getPort()),
            new UsernamePasswordCredentials(username, password)
        );
        return this;
    }

    /**
     * Collects data from the associated resource.
     * @return The HTTP response
     * @throws IOException If there is an issue executing the HTTP request
     */
    public CloseableHttpResponse response() throws IOException {
        final URI uri = this.uri(this.url);
        final CloseableHttpResponse response;
        if (this.provider == null) {
            final HttpUriRequest request = new HttpGet(uri);
            this.parameters(Input.Location.HEADER)
                .forEach(p -> request.addHeader(p.getName(), p.getValue()));
            response = Request.CLIENT.execute(request);
        } else {
            final HttpRequest request = new HttpPost(uri);
            this.parameters(Input.Location.HEADER)
                .forEach(p -> request.addHeader(p.getName(), p.getValue()));
            final HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(this.provider);
            final HttpHost host =
                new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            response = Request.CLIENT.execute(host, request, context);
        }
        Request.LOGGER.debug("{}", uri);
        return response;
    }

    /**
     * Collects the data from the associated URL.
     * @return The collected content
     * @throws IOException If there is a request execution error
     * @throws UnexpectedResponseCodeException If the response code is different than 200
     */
    public String data() throws IOException, UnexpectedResponseCodeException {
        try (CloseableHttpResponse response = this.response()) {
            if (response.getStatusLine().getStatusCode() != Request.OK_CODE) {
                throw new UnexpectedResponseCodeException(
                    "Unexpected response code '%s' from URL '%s'.",
                    response.getStatusLine().getStatusCode(),
                    this.url
                );
            }
            return this.asString(response.getEntity().getContent());
        }
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
        this.parameters(Input.Location.QUERY, Input.Location.FORM_DATA)
            .filter(param -> param.getValue() != null)
            .forEach(param -> {
                for (final String value : param.getValue().split(",")) {
                    builder.addParameter(param.getName(), value);
                }
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
