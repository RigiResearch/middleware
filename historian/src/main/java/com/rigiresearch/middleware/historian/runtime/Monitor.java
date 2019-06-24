package com.rigiresearch.middleware.historian.runtime;

import it.sauronsoftware.cron4j.Scheduler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A polling monitor to collect resources from a REST API.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class Monitor implements Runnable, Callable<Void> {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The default buffer size.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * The URL this monitor queries.
     */
    private final URL url;

    /**
     * A list of parameters for the API requests.
     */
    private final List<Parameter> params;

    /**
     * A cron expression for scheduling periodic requests.
     */
    private final String expression;

    /**
     * The task scheduler.
     */
    private final Scheduler scheduler;

    /**
     * The identifier of the scheduled task.
     */
    private String identifier = "";

    /**
     * Schedules the periodic requests.
     */
    @Override
    public void run() {
        final URI uri = this.uri();
        this.identifier = this.scheduler.schedule(this.expression, () -> {
            final CloseableHttpClient client = HttpClients.createDefault();
            final HttpUriRequest request = new HttpGet(uri);
            this.parameters(Parameter.Location.HEADER)
                .forEach(p -> request.addHeader(p.name(), p.value()));
            try {
                final CloseableHttpResponse response = client.execute(request);
                // Instantiate the schema class based on the response content
                Monitor.LOGGER.info(response.getEntity().getContentType());
                Monitor.LOGGER.info(
                    this.string(response.getEntity().getContent()));
            } catch (final IOException exception) {
                Monitor.LOGGER.error("Request execution error", exception);
            }
        });
    }

    /**
     * Schedules the periodic requests. See {@link #run()}.
     * @return Null
     * @throws Exception Never
     */
    @Override
    public Void call() throws Exception {
        this.run();
        return null;
    }

    /**
     * Removes future requests from the scheduler. <b>Note</b> that this method
     * does not interrupt ongoing requests.
     */
    public void stop() {
        this.scheduler.deschedule(this.identifier);
    }

    /**
     * Builds the target URI replacing the parameters where corresponds.
     * @return A URI with the corresponding parameters set
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private URI uri() {
        final List<String> flatpath = Arrays.asList(this.url.getPath());
        this.parameters(Parameter.Location.PATH)
            .forEach(parameter -> {
                flatpath.set(
                    0,
                    flatpath.get(0).replaceAll(
                        String.format("\\{%s\\}", parameter.name()),
                        parameter.value()
                    )
                );
            });
        final URIBuilder builder = new URIBuilder()
            .setScheme(this.url.getProtocol())
            .setHost(this.url.getHost())
            .setPath(flatpath.get(0));
        this.parameters(
            Parameter.Location.QUERY,
            Parameter.Location.FORM_DATA
        ).forEach(param -> {
            builder.setParameter(param.name(), param.value());
        });
        try {
            return builder.build();
        } catch (final URISyntaxException exception) {
            Monitor.LOGGER.error("Malformed URI", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Filters out the params based on the given parameter locations.
     * @param criteria The filtering criteria
     * @return A stream of parameters
     */
    private Stream<Parameter> parameters(final Parameter.Location... criteria) {
        return this.params.stream()
            .filter(p -> Arrays.asList(criteria).contains(p.location()));
    }

    /**
     * Returns the string representation of the given stream.
     * @param stream The input stream
     * @return A UTF-8 string
     * @throws IOException if something bad happens!
     */
    private String string(final InputStream stream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[Monitor.BUFFER_SIZE];
        int length = stream.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = stream.read(buffer);
        }
        return result.toString(StandardCharsets.UTF_8.toString());
    }
}
