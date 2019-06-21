package com.rigiresearch.middleware.historian.runtime;

import lombok.Data;

/**
 * A request parameter.
 * @see Location
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Data
public final class Parameter {

    /**
     * The name.
     */
    private final String name;

    /**
     * The value.
     */
    private final String value;

    /**
     * Location of a request parameter.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    public enum Location {
        /**
         * The message body.
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
