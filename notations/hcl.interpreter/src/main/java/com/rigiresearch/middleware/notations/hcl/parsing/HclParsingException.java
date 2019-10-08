package com.rigiresearch.middleware.notations.hcl.parsing;

/**
 * Parsing exception for syntactic and semantic errors.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclParsingException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -8216525916901373011L;

    /**
     * Default constructor.
     * @param message The error message (format)
     * @param args Arguments to the message format
     */
    public HclParsingException(final String message,
        final Object... args) {
        super(String.format(message, args));
    }

}
