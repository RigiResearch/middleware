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
    private static final String REGULAR = "constant-scenario";

    /**
     * The jMeter test plan for the linear scenario.
     */
    private static final String LINEAR = "linear-scenario";

    /**
     * The jMeter test plan for the spike scenario.
     */
    private static final String SPIKE = "spike-scenario";

    /**
     * Default constructor.
     * @param directory The working directory
     * @throws IOException If there is an I/O error
     */
    public JMeterClient(final File directory)
        throws IOException {
        super(JMeterClient.EXECUTABLE, directory);
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
     * @param scenario The scenario to run
     * @param variant The deployed variant
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean run(final Scenario scenario, final SoftwareVariant variant,
        final long timeout, final TimeUnit unit) throws InterruptedException,
        IOException, TimeoutException {
        final String name = String.format(
            "%s-%s",
            scenario.scenarioName(),
            variant.getName().variantName()
        );
        final List<String> args = new ArrayList<>();
        args.add("-t");
        args.add(scenario.file());
        args.add("-l");
        args.add(String.format("%s.csv", name));
        args.add("-e");
        args.add("-o");
        args.add(name);
        return this.run("-n", args, timeout, unit) == 0;
    }

    /**
     * Possible execution scenarios.
     */
    public enum Scenario {
        REGULAR(JMeterClient.REGULAR),
        LINEAR(JMeterClient.LINEAR),
        SPIKE(JMeterClient.SPIKE);

        /**
         * The scenario file name.
         */
        private final String name;

        Scenario(String file) {
            this.name = file;
        }

        /**
         * The scenario file name.
         * @return A non-empty, non-null string
         */
        public String file() {
            return String.format("%s.jmx", this.name);
        }

        /**
         * The name of the scenario, without the file extension.
         * @return A non-null, non-empty string
         */
        public String scenarioName() {
            return this.name;
        }
    }

}
