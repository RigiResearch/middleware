package com.rigiresearch.middleware.experimentation.infrastructure;

import com.rigiresearch.middleware.experimentation.util.TerraformClient;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
     * An empty string.
     */
    private static final String EMPTY = "";

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
                        .getResourceAsStream("templates/terraform.tfvars.st")
                ).readAllBytes()
            ),
            '#',
            '#'
        );
        template.add("nodes", this.data[0]);
        template.add("memory", String.format("\"%dg\"", this.data[1]));
        template.add("cpus", this.data[2]);
        return template.render();
    }

    /**
     * Saves the deployment files.
     * @return This object
     * @throws IOException If there is a problem writing the file
     */
    public Deployment save() throws IOException {
        this.save0("main.tf", this.main());
        this.save0("terraform.tfvars", this.inputs());
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
     */
    public Future<Deployment> deploy() {
        final String id = this.identifier();
        final File current = new File(String.format("%s/%s", Deployment.DIRECTORY, id));
        return CompletableFuture.supplyAsync(() -> {
            try (TerraformClient client = new TerraformClient(current)) {
                if (client.version(10L, TimeUnit.SECONDS)
                    && client.init(2L, TimeUnit.MINUTES)
                    && client.plan(2L, TimeUnit.MINUTES)) {
                    // TODO Apply changes and then compute application metrics
                    // && client.apply(10L, TimeUnit.MINUTES)) {
                    client.destroy(5L, TimeUnit.MINUTES);
                } else {
                    this.erroneous = true;
                    Deployment.LOGGER.info("{} There was an error running init/plan", id);
                }
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
        return String.format("%d-%d-%d", this.data[0], this.data[1], this.data[2]);
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
