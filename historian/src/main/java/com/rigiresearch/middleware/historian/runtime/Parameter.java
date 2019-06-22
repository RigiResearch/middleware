package com.rigiresearch.middleware.historian.runtime;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * A request parameter.
 * @see Location
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@EqualsAndHashCode
@Getter
@ToString
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
     * The location.
     */
    private final Location location;

    /**
     * Secondary constructor.
     * @param name The parameter name
     * @param location The parameter location
     */
    public Parameter(final String name, final Location location) {
        this(name, "", location);
    }

    /**
     * Default constructor.
     * @param name The parameter name
     * @param value The parameter value
     * @param location The parameter location
     */
    public Parameter(final String name, final String value,
        final Location location) {
        this.name = name;
        this.value = value;
        this.location = location;
    }

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
