package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jOutputStream;

/**
 * A Terraform client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class TerraformClient {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(TerraformClient.class);

    /**
     * Regex to match ANSI color codes.
     */
    private static final Pattern ANSI_REGEX = Pattern.compile("\\u001b[^m]*?m");

    /**
     * Terraform's executable name.
     */
    private static final String EXECUTABLE = "terraform";

    /**
     * The working directory.
     */
    private final File directory;

    /**
     * An output stream for the output log.
     */
    private final OutputStream output;

    /**
     * An output stream for the error log.
     */
    private final OutputStream error;

    /**
     * Default constructor.
     * @param directory The working directory
     * @throws IOException If there is an I/O error
     */
    public TerraformClient(final File directory) throws IOException {
        this.directory = directory;
        this.output = this.stream("output.txt");
        this.error = this.stream("error.txt");
    }

    /**
     * Creates an output stream as well as a file output stream.
     * @param filename The log file
     * @return A non-null output stream
     * @throws IOException If there is an I/O error
     */
    private OutputStream stream(final String filename) throws IOException {
        // Do not close this file stream
        final FileOutputStream out =
            new FileOutputStream(new File(this.directory, filename));
        return new Slf4jOutputStream(TerraformClient.LOGGER) {
            @Override
            protected void processLine(final String line) {
                TerraformClient.LOGGER.debug(line);
                final String tmp = TerraformClient.ANSI_REGEX.matcher(line).replaceAll("");
                try {
                    out.write(
                        String.format("%s\n", tmp)
                            .getBytes(StandardCharsets.UTF_8)
                    );
                } catch (final IOException exception) {
                    TerraformClient.LOGGER.error(exception.getMessage(), exception);
                    throw new IllegalStateException(exception);
                }
            }
        };

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
        return this.run("apply", timeout, unit) == 0;
    }

    /**
     * Runs a Terraform command.
     * @param command The command to run
     * @param time The timout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    private int run(final String command, final long time, final TimeUnit unit)
        throws InterruptedException, TimeoutException, IOException {
        return this.run(command, Collections.emptyMap(), time, unit);
    }

    /**
     * Runs a Terraform command.
     * @param command The command to run
     * @param args Additional arguments
     * @param time The timout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    private int run(final String command, final Map<String, String> args,
        final long time, final TimeUnit unit)
        throws InterruptedException, TimeoutException, IOException {
        final List<String> commands = new ArrayList<>(args.size() + 1);
        commands.add(TerraformClient.EXECUTABLE);
        commands.add(command);
        commands.addAll(
            args.entrySet()
                .stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.toList())
        );
        final String formatted = String.format("\n$ %s\n", String.join(" ", commands));
        this.output.write(formatted.getBytes());
        this.output.flush();
        return new ProcessExecutor()
            .command(commands)
            .directory(this.directory)
            .redirectOutput(this.output)
            .redirectError(this.error)
            .timeout(time, unit)
            .execute()
            .getExitValue();
    }

}
