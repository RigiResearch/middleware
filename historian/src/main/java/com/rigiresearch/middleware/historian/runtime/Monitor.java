package com.rigiresearch.middleware.historian.runtime;

import java.net.URL;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * A polling monitor to collect resources from a REST API.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class Monitor extends Thread {

    /**
     * The URL this monitor queries.
     */
    private final URL path;

    /**
     * A list of parameters for the API requests.
     */
    private final List<Parameter> parameters;

    /**
     * A cron expression for scheduling periodic requests.
     */
    private final String expression;

    /**
     * Schedule the periodic requests.
     */
    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes future requests from the scheduler. <b>Note</b> that this method
     * does not interrupt ongoing requests.
     */
    public void halt() {
        throw new UnsupportedOperationException();
    }
}
