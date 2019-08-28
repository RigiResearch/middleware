package com.rigiresearch.middleware.historian.runtime;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.historian.runtime.graph.Monitor;
import it.sauronsoftware.cron4j.Scheduler;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authentication provider.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class ApiKeyProvider {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(ApiKeyProvider.class);

    /**
     * A unique name.
     */
    private final String id;

    /**
     * The configuration.
     */
    private final Configuration config;

    /**
     * The graph to which the authentication data is "injected".
     */
    private final Graph<Monitor> graph;

    /**
     * The scheduler instance.
     */
    private final Scheduler scheduler;

    /**
     * Setup and schedule this provider.
     * @throws IOException See {@link #collect()}
     * @throws UnexpectedResponseCodeException See {@link #collect()}
     */
    public void setup() throws IOException, UnexpectedResponseCodeException {
        final String expression =
            this.config.getString(String.format("auth.%s.periodicity", this.id));
        // Perform the first call to ensure having a token, now and then schedule it
        this.collect();
        this.scheduler.schedule(expression, () -> {
            try {
                this.collect();
            } catch (final IOException | UnexpectedResponseCodeException exception) {
                ApiKeyProvider.LOGGER.error(exception.getMessage(), exception);
                throw new IllegalStateException(exception);
            }
        });
    }

    /**
     * Collect and set the data.
     * @throws IOException If the URL is malformed
     * @throws UnexpectedResponseCodeException If the response is not successful
     */
    private void collect() throws IOException, UnexpectedResponseCodeException {
        final String url =
            this.config.getString(String.format("auth.%s.url", this.id));
        final String selector =
            this.config.getString(String.format("auth.%s.selector", this.id));
        final String username =
            this.config.getString(String.format("auth.%s.username", this.id));
        final String password =
            this.config.getString(String.format("auth.%s.password", this.id));
        final String name =
            this.config.getString(String.format("auth.%s.input", this.id));
        final String data = new Request(Collections.emptyList(), new URL(url))
            .withCredentials(username, password)
            .data();
        final String token = new XpathValue(data, selector).singleValue();
        ApiKeyProvider.LOGGER.debug("Using auth token {}", token);
        synchronized (this.graph) {
            for (final Monitor monitor : this.graph.getNodes()) {
                monitor.setValue(name, token);
            }
        }
    }

}
