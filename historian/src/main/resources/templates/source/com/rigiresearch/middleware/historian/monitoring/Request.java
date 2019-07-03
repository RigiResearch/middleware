package com.rigiresearch.middleware.historian.monitoring;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple HTTP request.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@Getter
@RequiredArgsConstructor
public final class Request implements Cloneable {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Request.class);

    /**
     * The URL this monitor queries.
     */
    private final URL url;

    /**
     * A list of input parameters for the API requests.
     */
    private final List<Input> inputs;

    /**
     * A credentials provider (optional).
     */
    private final CredentialsProvider provider;

    /**
     * Makes a request to the specified URL with the corresponding parameters
     * set.
     * @return The request's response
     * @throws IOException If there is a problem with the connection
     */
    public CloseableHttpResponse response() throws IOException {
        final URI uri = this.uri();
        final CloseableHttpClient client = HttpClients.createDefault();
        final CloseableHttpResponse response;
        if (this.provider != null) {
            Request.LOGGER.debug(
                "Credentials provider found for path '{}'", uri.getPath()
            );
            final HttpRequest request = new HttpPost(uri);
            this.parameters(Input.Location.HEADER)
                .forEach(p -> request.addHeader(p.name(), p.value().get()));
            final HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(this.provider);
            final HttpHost host =
                new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            response = client.execute(host, request, context);
        } else {
            final HttpUriRequest request = new HttpGet(uri);
            this.parameters(Input.Location.HEADER)
                .forEach(p -> request.addHeader(p.name(), p.value().get()));
            response = client.execute(request);
        }
        Request.LOGGER.debug(uri.getPath());
        return response;
    }

    /**
     * Filters out the inputs based on the given parameter locations.
     * @param criteria The filtering criteria
     * @return A stream of parameters
     */
    private Stream<Input> parameters(final Input.Location... criteria) {
        return this.inputs.stream()
            .filter(p -> Arrays.asList(criteria).contains(p.location()));
    }

    /**
     * Builds the target URI replacing the parameters where corresponds.
     * @return A URI with the corresponding parameters set
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private URI uri() {
        final List<String> flatpath = Arrays.asList(this.url.getPath());
        this.parameters(Input.Location.PATH)
            .forEach(input -> {
                flatpath.set(
                    0,
                    flatpath.get(0).replaceAll(
                        String.format("\\{%s\\}", input.name()),
                        input.value().get()
                    )
                );
            });
        final URIBuilder builder = new URIBuilder()
            .setScheme(this.url.getProtocol())
            .setHost(this.url.getHost())
            .setPath(flatpath.get(0));
        this.parameters(
            Input.Location.QUERY,
            Input.Location.FORM_DATA
        ).forEach(param -> {
            builder.setParameter(param.name(), param.value().get());
        });
        try {
            return builder.build();
        } catch (final URISyntaxException exception) {
            Request.LOGGER.error("Malformed URI", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    protected Request clone() {
        return new Request(
            this.url,
            this.inputs.stream()
                .map(in -> in.clone())
                .collect(Collectors.toList()),
            this.provider
        );
    }

}
