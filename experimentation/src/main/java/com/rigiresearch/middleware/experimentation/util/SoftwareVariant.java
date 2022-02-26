package com.rigiresearch.middleware.experimentation.util;

import lombok.Value;

/**
 * Configuration of a software variant.
 * @author Miguel Jimenez (accounts+gitlab.lumiers.8765@migueljimenez.co)
 * @version $Id$
 * @since 0.11.0
 */
@Value
public class SoftwareVariant {

    /**
     * The variant's name.
     */
    VariantName name;

    /**
     * The corresponding Kubernete's service name.
     */
    String service;

    /**
     * The corresponding Kubernete's service port.
     */
    int port;

    public enum VariantName {
        PROXY_CACHE_3_1("proxy-cache-3.1");

        /**
         * The variant's name.
         */
        String name;

        /**
         * Default constructor.
         * @param name The variant's name.
         */
        VariantName(final String name) {
            this.name = name;
        }

        /**
         * Gets the variant's name.
         * @return a non-null, non-empty string
         */
        public String variantName() {
            return this.name;
        }
    }

}
