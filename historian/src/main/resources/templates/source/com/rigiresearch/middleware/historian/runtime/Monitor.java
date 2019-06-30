package com.rigiresearch.middleware.historian.runtime;

import it.sauronsoftware.cron4j.Scheduler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A polling monitor to collect resources from a REST API.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
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
     * HTTP 200 status code.
     */
    private static final int OK_CODE = 200;

    /**
     * The corresponding path's id.
     */
    @Getter
    private final String name;

    /**
     * The task scheduler.
     */
    private final Scheduler scheduler;

    /**
     * The request this monitor performs.
     */
    private final Request request;

    /**
     * A cron expression for scheduling periodic requests.
     */
    private final Supplier<String> expression;

    /**
     * A list of output parameters associated with this monitor's path.
     */
    private final List<OutputParameter> outputs;

    /**
     * The identifier of the scheduled task.
     */
    private String identifier = "";

    /**
     * Schedules the periodic requests.
     */
    @Override
    public void run() {
        Monitor.LOGGER.info("Scheduling monitor '{}'", this.name);
        this.identifier = this.scheduler.schedule(
            this.expression.get(),
            () -> this.collect()
        );
    }

    /**
     * Schedules the periodic requests. See {@link #run()}.
     * @return Null
     */
    @Override
    public Void call() {
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
     * Collects the data from the API.
     */
    public void collect() {
        try {
            final CloseableHttpResponse response = this.request.response();
            if (response.getStatusLine().getStatusCode() == Monitor.OK_CODE) {
                final String content = this.string(response.getEntity().getContent());
                final String type = response.getEntity().getContentType().getValue();
                this.outputs.stream()
                    .forEach(param -> {
                        try {
                            param.update(content, type);
                        } catch (final IOException exception) {
                            Monitor.LOGGER.error(exception);
                        }
                    });
                // TODO Instantiate the schema class based on the response content
                Monitor.LOGGER.info(content.substring(0, Math.min(content.length(), 300)));
            } else {
                Monitor.LOGGER.error(
                    "Unexpected response code '{}'",
                    response.getStatusLine().getStatusCode()
                );
            }
        } catch (final IOException exception) {
            Monitor.LOGGER.error("Request execution error", exception);
        }
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
