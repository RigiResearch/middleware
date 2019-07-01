package com.rigiresearch.middleware.historian.runtime;

import it.sauronsoftware.cron4j.Scheduler;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
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
     * The corresponding path's id.
     */
    @Getter
    private final String name;

    /**
     * The task scheduler.
     */
    private final Scheduler scheduler;

    /**
     * A cron expression for scheduling periodic requests.
     */
    private final Supplier<String> expression;

    /**
     * A data collector.
     */
    @Getter
    private final Collector collector;

    /**
     * The identifier of the scheduled task.
     */
    private String identifier = "";

    /**
     * Schedules the periodic requests.
     */
    @Override
    public void run() {
        Monitor.LOGGER.debug("Scheduling monitor '{}'", this.name);
        this.identifier = this.scheduler.schedule(
            this.expression.get(),
            () -> this.collector.collect()
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

}
