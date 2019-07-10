package com.rigiresearch.middleware.historian.monitoring;

/**
 * This exception is thrown when a request's response code was unexpected.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class UnexpectedResponseCode extends Exception {

    /**
     * Default constructor.
     * @param message The error message (format)
     * @param args Arguments to the message format
     */
    public UnexpectedResponseCode(final String message, final Object... args) {
        super(String.format(message, args));
    }

}
