package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Terraform client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class TerraformClient extends AbstractCommandLineClient {

    /**
     * Terraform's executable name.
     */
    private static final String EXECUTABLE = "terraform";

    /**
     * Default constructor.
     * @param directory The working directory
     * @throws IOException If there is an I/O error
     */
    public TerraformClient(final File directory) throws IOException {
        super(TerraformClient.EXECUTABLE, directory);
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
        return this.run("version", timeout, unit) == 0;
    }

    /**
     * Runs the "init" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean init(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        return this.run("init", timeout, unit) == 0;
    }

    /**
     * Runs the "plan" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean plan(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        return this.run("plan", timeout, unit) == 0;
    }

    /**
     * Runs the "plan" command.
     * @param name The output's name
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public String output(final String name, final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        final List<String> args = List.of(TerraformClient.EXECUTABLE, "output", name);
        return this.runAndGetOutput(args, timeout, unit);
    }

    /**
     * Runs the "apply" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean apply(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        final List<String> args = new ArrayList<>(1);
        args.add("-auto-approve");
        return this.run("apply", args, timeout, unit) == 0;
    }

    /**
     * Runs the "destroy" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean destroy(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        final List<String> args = new ArrayList<>(1);
        args.add("-auto-approve");
        return this.run("destroy", args, timeout, unit) == 0;
    }

}
