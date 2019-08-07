package com.rigiresearch.middleware.historian.monitoring;

import lombok.Value;

/**
 * A request parameter.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Value
public final class Input {

    /**
     * The name.
     */
    private final String name;

    /**
     * The value supplier.
     */
    private final String value;

    /**
     * Whether this input is required.
     */
    private final boolean required;

    /**
     * The location.
     */
    private final Location location;

    /**
     * Location of a request parameter.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    public enum Location {
        /**
         * The message body.<br />
         * <b>Note</b> that since the requests are GET, this constant is not
         * currently being used.
         */
        BODY,

        /**
         * Form parameters.
         */
        FORM_DATA,

        /**
         * The header fields.
         */
        HEADER,

        /**
         * The path part of the URL.
         */
        PATH,

        /**
         * The query part of the URL.
         */
        QUERY
    }
}

