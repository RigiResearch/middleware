package com.rigiresearch.middleware.historian.runtime;

/**
 * This exception is thrown when a request's response code was unexpected.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class UnexpectedResponseCodeException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3911876210328970030L;

    /**
     * Default constructor.
     * @param message The error message (format)
     * @param args Arguments to the message format
     */
    public UnexpectedResponseCodeException(final String message, final Object... args) {
        super(String.format(message, args));
    }

}
