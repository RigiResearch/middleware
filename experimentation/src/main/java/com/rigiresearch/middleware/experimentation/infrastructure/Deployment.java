package com.rigiresearch.middleware.experimentation.infrastructure;

import com.microsoft.terraform.TerraformClient;
import com.microsoft.terraform.TerraformOptions;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
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
     * An HCL parser.
     */
    private static final HclParser PARSER = new HclParser();

    /**
     * The base deployment specification.
     */
    private final Specification specification;

    /**
     * The chromosome data.
     */
    private final int[] data;

    /**
     * Default constructor.
     * @param data The chromosome data
     * @throws HclParsingException If there is a problem interpreting the template
     * @throws IOException If the template file is not found
     */
    public Deployment(final int[] data) throws HclParsingException, IOException {
        this.data = data;
        this.specification = Deployment.PARSER.parse(
            new String(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("templates/main.tf")
                ).readAllBytes()
            )
        );
    }

    /**
     * Render the chromosome data as the main template file.
     * @return The contents of the main file
     */
    public String main() {
        // The template can be manipulated before rendering
        return Deployment.PARSER.parse(this.specification);
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
            new File("deployment"),
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
     */
    public Future<Deployment> deploy() {
        final File current = new File(String.format("deployment/%s", this.identifier()));
        final TerraformOptions options = new TerraformOptions();
        final TerraformClient client = new TerraformClient(options);
        client.setOutputListener(Deployment.LOGGER::debug);
        client.setErrorListener(Deployment.LOGGER::error);
        client.setWorkingDirectory(current);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Deployment.LOGGER.debug(client.version().get());
                client.plan().get();
                client.apply().get();
                // TODO deploy the case study and run performance tests
                client.destroy().get();
                client.close();
            } catch (final Exception exception) {
                throw new IllegalStateException(exception);
            }
            return this;
        });
    }

    /**
     * Measure the execution and assign a score to the deployment based on the
     * measured service latency.
     * @return A positive number
     */
    public double score() {
        return 0.0;
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

}
