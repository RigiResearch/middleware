package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A very simple Rscript client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.11.0
 */
public class RScriptClient extends AbstractCommandLineClient {

    /**
     * This client's executable;
     */
    private static final String EXECUTABLE = "Rscript";

    /**
     * Default constructor.
     * @param directory The working directory
     * @throws IOException If there is an I/O error
     */
    public RScriptClient(final File directory) throws IOException {
        super(RScriptClient.EXECUTABLE, directory);
    }

    /**
     * Runs a command.
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @param args The command and its arguments arguments
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public String output(final long time, final TimeUnit unit, final String... args)
        throws InterruptedException, TimeoutException, IOException {
        final List<String> parts = new ArrayList<>();
        parts.add(RScriptClient.EXECUTABLE);
        parts.addAll(
            Arrays.stream(args)
                .collect(Collectors.toList())
        );
        return this.runAndGetOutput(parts, time, unit);
    }

}
