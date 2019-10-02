package com.rigiresearch.middleware.graph;

import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * An adapter for having multiline values as inputs.
 * Based on <a href="https://stackoverflow.com/a/32629531">this StackOverflow
 * answer</a>.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
public final class InputValueAdapter
    extends XmlAdapter<InputValueAdapter.AdaptedValue, String> {

    @Override
    public String unmarshal(final AdaptedValue value) {
        return value.getValue();
    }

    @Override
    public AdaptedValue marshal(final String value) {
        return new AdaptedValue(value);
    }

    /**
     * An adapted value wrapping a {@link String} value.
     */
    static final class AdaptedValue {

        /**
         * The element's value.
         */
        @XmlValue
        private String value;

        /**
         * Empty constructor.
         */
        AdaptedValue() {
            // Nothing to do here
        }

        /**
         * Default constructor.
         * @param value The element's value
         */
        AdaptedValue(final String value) {
            this.value = value;
        }

        /**
         * The element's value.
         * @return A non-null String value
         */
        String getValue() {
            return this.value;
        }

    }

}
