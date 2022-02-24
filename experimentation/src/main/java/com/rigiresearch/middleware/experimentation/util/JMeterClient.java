package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A simple jMeter client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.11.0
 */
public class JMeterClient extends AbstractCommandLineClient {

    /**
     * jMeter's executable name.
     */
    private static final String EXECUTABLE = "jmeter";

    /**
     * The jMeter test plan for the regular scenario.
     */
    private static final String REGULAR_FILE = "constant-scenario.jmx";

    /**
     * The jMeter test plan for the linear scenario.
     */
    private static final String LINEAR_FILE = "linear-scenario.jmx";

    /**
     * The jMeter test plan for the spike scenario.
     */
    private static final String SPIKE_FILE = "spike-scenario.jmx";

    /**
     * Directory containing the execution scenarios.
     */
    private final File scenarios;

    /**
     * Default constructor.
     * @param directory The working directory
     * @param scenarios Directory containing the execution scenarios
     * @throws IOException If there is an I/O error
     */
    public JMeterClient(final File directory, final File scenarios)
        throws IOException {
        super(JMeterClient.EXECUTABLE, directory);
        this.scenarios = scenarios;
    }

    /**
     * Runs the "version" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean version(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        return this.run("--version", timeout, unit) == 0;
    }

    /**
     * Runs the Regular execution scenario command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean run(final Scenario scenario, final long timeout,
        final TimeUnit unit) throws InterruptedException, IOException, TimeoutException {
        final File file = new File(this.scenarios, scenario.file());
        final List<String> args = new ArrayList<>(1);
        args.add("-t");
        args.add(file.getAbsolutePath());
        return this.run("", args, timeout, unit) == 0;
    }

    /**
     * Possible execution scenarios.
     */
    public enum Scenario {
        REGULAR(JMeterClient.REGULAR_FILE),
        LINEAR(JMeterClient.LINEAR_FILE),
        SPIKE(JMeterClient.SPIKE_FILE);

        /**
         * The scenario file name.
         */
        private final String file;

        Scenario(String file) {
            this.file = file;
        }

        /**
         * The scenario file name.
         * @return A non-empty, non-null string
         */
        public String file() {
            return this.file;
        }
    }

}
