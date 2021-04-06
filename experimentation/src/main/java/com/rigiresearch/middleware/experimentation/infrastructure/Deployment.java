package com.rigiresearch.middleware.experimentation.infrastructure;

import com.microsoft.terraform.TerraformClient;
import com.microsoft.terraform.TerraformOptions;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

/**
 * A deployment template.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Deployment {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Deployment.class);

    /**
     * The directory where results results are stored.
     */
    private static final String DIRECTORY = "deployments";

    /**
     * Regex to match ANSI color codes.
     */
    private static final Pattern ANSI_REGREX = Pattern.compile("\\u001b[^m]*?m");

    /**
     * The base deployment specification.
     */
    private final String specification;

    /**
     * The chromosome data.
     */
    private final int[] data;

    /**
     * Whether an error happened while performing the deployment.
     */
    private boolean erroneous;

    /**
     * Default constructor.
     * @param data The chromosome data
     * @throws HclParsingException If there is a problem interpreting the template
     * @throws IOException If the template file is not found
     */
    public Deployment(final int[] data) throws HclParsingException, IOException {
        this.data = data;
        this.specification = new String(
            Objects.requireNonNull(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("templates/main.tf")
            ).readAllBytes()
        );
    }

    /**
     * Render the chromosome data as the main template file.
     * @return The contents of the main file
     */
    public String main() {
        return this.specification;
    }

    /**
     * Render the chromosome data as template inputs.
     * @return The contents of the inputs file
     * @throws IOException If there is a problem reading the inputs file
     */
    public String inputs() throws IOException {
        final ST template = new ST(
            new String(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("templates/inputs.tf.st")
                ).readAllBytes()
            ),
            '#',
            '#'
        );
        template.add("nodes", this.data[0]);
        template.add("memory", String.format("\"%dg\"", this.data[1]));
        template.add("cpus", this.data[2]);
        template.add("network", String.format("\"%dMb\"", this.data[3]));
        return template.render();
    }

    /**
     * Saves the deployment files.
     * @return This object
     * @throws IOException If there is a problem writing the file
     */
    public Deployment save() throws IOException {
        this.save0("main.tf", this.main());
        this.save0("inputs.tf", this.inputs());
        return this;
    }

    /**
     * Saves the deployment files.
     * @param name The file name
     * @param content The file content
     * @throws IOException If there is a problem writing the file
     */
    private void save0(final String name, final String content) throws IOException {
        final File directory = new File(
            new File(Deployment.DIRECTORY),
            this.identifier()
        );
        directory.mkdirs();
        Files.write(
            new File(directory, name).toPath(),
            content.getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    /**
     * Perform this deployment.
     * @return This object
     * @throws IOException If the output or error files cannot be created
     */
    public Future<Deployment> deploy() throws IOException {
        final File current = new File(
            String.format("%s/%s", Deployment.DIRECTORY, this.identifier())
        );
        final TerraformOptions options = new TerraformOptions();
        final TerraformClient client = new TerraformClient(options);
        client.setOutputListener(this.listener(new File(current, "output.txt"), false));
        client.setErrorListener(this.listener(new File(current, "error.txt"), true));
        client.setWorkingDirectory(current);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (client.version().get() != null
                    && client.plan().get()
                    && client.apply().get()) {
                    // TODO deploy the case study and run performance tests
                    client.destroy().get();
                } else {
                    this.erroneous = true;
                }
                client.close();
            } catch (final Exception exception) {
                throw new IllegalStateException(exception);
            }
            return this;
        });
    }

    /**
     * Saves content to a file.
     * @param file The target file
     * @param error Whether this listener is for errors
     * @return A non-null string consumer
     */
    private Consumer<String> listener(final File file, final boolean error) {
        return content -> {
            if (error) {
                Deployment.LOGGER.error(
                    "[{}] {}", file.getParentFile().getName(), content);
            }
            content = Deployment.ANSI_REGREX.matcher(content).replaceAll("");
            try {
                Files.write(
                    file.toPath(),
                    String.format("%s\n", content).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
                );
            } catch (final IOException exception) {
                Deployment.LOGGER
                    .error(exception.getLocalizedMessage(), exception);
                throw new IllegalStateException(exception);
            }
        };

    }

    /**
     * Measure the execution and assign a score to the deployment based on the
     * measured service latency.
     * @return A positive number
     */
    public Deployment.Score score() {
        final double value;
        if (this.erroneous) {
            value = Double.MAX_VALUE;
        } else {
            value = 0.0;
        }
        return new Deployment.Score(this.erroneous, value);
    }

    /**
     * An identifier based on the chromosome being deployed.
     * @return A non-null, non-empty string
     */
    public String identifier() {
        return String.format(
            "%d-%d-%d-%d",
            this.data[0],
            this.data[1],
            this.data[2],
            this.data[3]
        );
    }

    /**
     * A deployment score.
     */
    @Value
    public static class Score {

        /**
         * Whether there was an error during the deployment and a score could
         * not be computed.
         */
        boolean error;

        /**
         * The deployment score. If there was an error, it is {@code Double.MAX_VALUE}.
         */
        double value;

    }

}
