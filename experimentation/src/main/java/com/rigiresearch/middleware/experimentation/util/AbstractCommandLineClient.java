package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jOutputStream;

/**
 * A command line client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public abstract class AbstractCommandLineClient implements AutoCloseable {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(AbstractCommandLineClient.class);

    /**
     * Regex to match ANSI color codes.
     */
    private static final Pattern ANSI_REGEX = Pattern.compile("\\u001b[^m]*?m");

    /**
     * Regex to match quotation marks.
     */
    private static final Pattern QUOTATION_REGEX = Pattern.compile("\"");

    /**
     * The executable name.
     */
    private final String executable;

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
     * Callbacks to close open resources.
     */
    private final List<Runnable> callbacks;

    /**
     * Default constructor.
     * @param executable The binary file name
     * @param directory The working directory
     * @throws IOException If there is an I/O error
     */
    public AbstractCommandLineClient(final String executable,
        final File directory) throws IOException {
        this.executable = executable;
        this.directory = directory;
        this.callbacks = new ArrayList<>();
        this.output = this.stream(
            String.format("%s-output.txt", this.getClass().getSimpleName())
        );
        this.error = this.stream(
            String.format("%s-error.txt", this.getClass().getSimpleName())
        );
    }

    /**
     * Creates an output stream as well as a file output stream.
     * @param filename The log file
     * @return A non-null output stream
     * @throws IOException If there is an I/O error
     */
    protected OutputStream stream(final String filename) throws IOException {
        // Do not close this file stream
        final FileOutputStream out =
            new FileOutputStream(new File(this.directory, filename));
        this.callbacks.add(() -> {
            try {
                out.close();
            } catch (final IOException ignored) {
            }
        });
        return new Slf4jOutputStream(AbstractCommandLineClient.LOGGER) {
            @Override
            protected void processLine(final String line) {
                AbstractCommandLineClient.LOGGER.debug(line);
                final String tmp = AbstractCommandLineClient.ANSI_REGEX.matcher(line)
                    .replaceAll("");
                try {
                    out.write(
                        String.format("%s\n", tmp)
                            .getBytes(StandardCharsets.UTF_8)
                    );
                } catch (final IOException exception) {
                    AbstractCommandLineClient.LOGGER.error(
                        exception.getMessage(),
                        exception
                    );
                    throw new IllegalStateException(exception);
                }
            }
        };

    }

    /**
     * Runs a command.
     * @param command The command to run
     * @param time The timout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    protected int run(final String command, final long time, final TimeUnit unit)
        throws InterruptedException, TimeoutException, IOException {
        return this.run(command, Collections.emptyList(), time, unit);
    }

    /**
     * Runs a command asynchronously.
     * @param command The command to run
     * @param args Additional arguments
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws IOException If there is an I/O error
     */
    protected StartedProcess start(final String command, final List<String> args,
        final long time, final TimeUnit unit) throws IOException {
        final List<String> parts = new ArrayList<>(args.size() + 3);
        parts.add(this.executable);
        parts.add(command);
        parts.addAll(args);
        return this.prepareProcess(parts, time, unit).start();
    }

    /**
     * Runs a command.
     * @param command The command to run
     * @param args Additional arguments
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    protected int run(final String command, final List<String> args,
        final long time, final TimeUnit unit)
        throws InterruptedException, TimeoutException, IOException {
        final List<String> commands = new ArrayList<>(args.size() + 2);
        commands.add(this.executable);
        commands.add(command);
        commands.addAll(args);
        return this.run(commands, time, unit);
    }

    /**
     * Runs a command and returns the output.
     * @param args The command and its arguments arguments
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    protected String runAndGetOutput(final List<String> args, final long time,
        final TimeUnit unit) throws InterruptedException, TimeoutException, IOException {
        return this.prepareProcess(args, time, unit)
            .readOutput(true)
            .execute()
            .outputUTF8();
    }

    /**
     * Runs a command.
     * @param args The command and its arguments arguments
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    protected int run(final List<String> args, final long time, final TimeUnit unit)
        throws InterruptedException, TimeoutException, IOException {
        return this.prepareProcess(args, time, unit)
            .execute()
            .getExitValue();
    }

    /**
     * Prepares a command.
     * @param args The command and its arguments arguments
     * @param time The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws IOException If there is an I/O error
     */
    protected ProcessExecutor prepareProcess(final List<String> args, final long time,
        final TimeUnit unit) throws IOException {
        final String formatted = String.format("\n$ %s\n", String.join(" ", args));
        this.output.write(formatted.getBytes());
        this.output.flush();
        return new ProcessExecutor()
            .command(args)
            .directory(this.directory)
            .redirectOutput(this.output)
            .redirectError(this.error)
            .timeout(time, unit);
    }

    @Override
    public void close() {
        this.callbacks.forEach(Runnable::run);
    }

}
