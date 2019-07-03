package com.rigiresearch.middleware.historian.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sauronsoftware.cron4j.Scheduler;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.Configuration;
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
public final class Monitor implements Runnable, Callable<Void>, Cloneable {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The corresponding path's id.
     */
    @Getter
    private final String name;

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * The task scheduler.
     */
    private final Scheduler scheduler;

    /**
     * A data collector.
     */
    @Getter
    private final Collector collector;

    /**
     * the dependent monitors.
     */
    private final List<DependentMonitor> dependent;

    /**
     * An executor service.
     */
    private final ExecutorService executor;

    /**
     * A JSON mapper.
     * TODO Add support for class mapping from XML.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * The identifier of the scheduled task.
     */
    private String identifier = "";

    /**
     * Schedules the periodic requests.
     */
    @Override
    public void run() {
        final String expression = this.config.getString(
            String.format("%s.expression", this.name)
        );
        Monitor.LOGGER.debug("Scheduling monitor '{}'", this.name);
        this.identifier = this.scheduler.schedule(expression, () -> this.collect());
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

    protected String collect() {
        final String fqn = String.format("%s.response.class", this.name);
        Class<?> clazz = Object.class;
        try {
            clazz = Class.forName(this.config.getString(fqn));
        } catch (final ClassNotFoundException exception) {
            Monitor.LOGGER.error(
                String.format(
                    "Class '%s' for monitor '%s' was not found", fqn, this.name
                ),
                exception
            );
        }
        final boolean process = this.config.getBoolean(
            String.format("%s.response.process", this.name),
            true
        );
        final String content = this.collector.collect();
        if (process) {
            // TODO Instantiate the schema class
            try {
                Object object = this.mapper.readValue(content, clazz);
            } catch (IOException exception) {
                Monitor.LOGGER.error(
                    String.format("Error mapping content to class '%s'", clazz),
                    exception
                );
            }
        }
        try {
            this.dependentCollect(content);
        } catch (final IOException exception) {
            Monitor.LOGGER.error(
                "Could not collect data from dependent monitors",
                exception
            );
        }
        return content;
    }

    /**
     * Collects data from dependent monitors.
     * @param content The content collected by this monitor
     * @throws IOException If something bad happens extracting the data
     */
    private void dependentCollect(final String content)
        throws IOException {
        for (final DependentMonitor child : this.dependent) {
            final String variable = this.config.getString(
                String.format(
                    "%s.children.%s.input",
                    this.name,
                    child.monitor()
                        .collector()
                        .path()
                )
            );
            final String selector = this.config.getString(
                String.format(
                    "%s.inputs.%s.selector",
                    child.monitor()
                        .collector()
                        .path(),
                    variable
                )
            );
            final Optional<Input> opin = child.monitor().collector()
                .request()
                .inputs()
                .stream()
                .filter(in -> in.name().equals(variable))
                .findFirst();
            if (opin.isPresent()) {
                final Input input = opin.get();
                final int index = child.monitor()
                    .collector()
                    .request()
                    .inputs()
                    .indexOf(input);
                new XpathValue(selector, content)
                    .values()
                    .stream()
                    .map(value -> {
                        final Monitor clone = child.monitor().clone();
                        final Input copy =
                            new Input(input.name(), () -> value, input.location());
                        clone.collector()
                            .request()
                            .inputs()
                            .set(index, copy);
                        return clone;
                    })
                    .forEach(clone -> this.executor.submit(clone::collect));
            } else {
                Monitor.LOGGER.error(
                    "Missing property '{}'. Dependent monitor won't collect any data.",
                    variable
                );
            }
        }
    }

    @Override
    public Monitor clone() {
        return new Monitor(
            this.name,
            this.config,
            this.scheduler,
            this.collector.clone(),
            this.dependent.stream()
                .map(DependentMonitor::clone)
                .collect(Collectors.toList()),
            this.executor
        );
    }

    /**
     * A dependent monitor.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    @Accessors(fluent = true)
    @Value
    public static final class DependentMonitor implements Cloneable {

        /**
         * An Xpath selector for extracting the particular input values.
         */
        private final String selector;

        /**
         * The dependent monitor.
         */
        private final Monitor monitor;

        @Override
        protected DependentMonitor clone() {
            return new DependentMonitor(
                this.selector,
                this.monitor.clone()
            );
        }

    }

}
