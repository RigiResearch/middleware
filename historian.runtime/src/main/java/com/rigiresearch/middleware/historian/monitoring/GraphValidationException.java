package com.rigiresearch.middleware.historian.monitoring;

/**
 * Validation error for {@link com.rigiresearch.middleware.graph.Graph}
 * instances.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class GraphValidationException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3911876210328970030L;

    /**
     * Default constructor.
     * @param message The error message (format)
     * @param args Arguments to the message format
     */
    public GraphValidationException(final String message,
        final Object... args) {
        super(String.format(message, args));
    }

}
