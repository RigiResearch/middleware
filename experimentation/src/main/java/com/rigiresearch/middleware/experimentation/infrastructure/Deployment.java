package com.rigiresearch.middleware.experimentation.infrastructure;

import com.google.common.collect.Lists;
import com.rigiresearch.middleware.experimentation.util.JMeterClient;
import com.rigiresearch.middleware.experimentation.util.KubernetesClient;
import com.rigiresearch.middleware.experimentation.util.TerraformClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
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
     * The directory where results results are stored.
     */
    private static final File SCENARIOS = new File("scenarios");

    /**
     * The kube config file created after the terraform deployment.
     */
    private static final String KUBECONFIG = "./kubeconfig";

    /**
     * The target cloud's actual deployment values.
     */
    private final CloudChromosome chromosome;

    /**
     * The name of the variant to deploy (included in the manifest name).
     */
    private final String variant;

    /**
     * The scenario to test.
     */
    private final JMeterClient.Scenario scenario;

    /**
     * Whether an error happened while performing the deployment.
     */
    private boolean erroneous;

    /**
     * Whether this deployment cannot be deployed to the target cloud.
     */
    private boolean unsupported;

    /**
     * Default constructor.
     * @param chromosome The chromosome data
     * @param variant The name of the variant to deploy (included in the manifest name)
     * @param scenario The scenario to test
     * @throws IOException If the template file is not found
     */
    public Deployment(final CloudChromosome chromosome, final String variant,
        final JMeterClient.Scenario scenario)
        throws IOException {
        this.chromosome = chromosome;
        this.variant = variant;
        this.scenario = scenario;
    }

    /**
     * Whether this deployment is supported by the target cloud.
     * @return {@code True} if the deployment can be executed, {@code False} otherwise
     */
    public boolean isSupported() {
        return this.chromosome.isSupported();
    }

    /**
     * Reads supporting files.
     * @return The contents of supporting files and their paths
     * @throws IOException If there is an error reading the files
     */
    public Map<String, String> supportingFiles() throws IOException {
        final List<String> names = Lists.newArrayList(
            "manifests/manifest.proxy-cache-3.1.yaml",
            "terraform/compartment.tf",
            "terraform/datasources.tf",
            "terraform/network.tf",
            "terraform/oke_cluster.tf",
            "terraform/oke_kube_config.tf",
            "terraform/outputs.tf",
            "terraform/provider.tf",
            "terraform/variables.tf"
        );
        final Map<String, String> files = new HashMap<>(names.size());
        for (final String name : names) {
            files.put(
                name,
                new String(
                    Objects.requireNonNull(
                        Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(
                                String.format("templates/%s", name)
                            )
                    ).readAllBytes()
                )
            );
        }
        return files;
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
        template.add("nodes", this.chromosome.actualNodes());
        template.add("memory", this.chromosome.formattedMemory());
        template.add("cpus", this.chromosome.actualCpus());
        // template.add("flavor", this.chromosome.flavor());
        return template.render();
    }

    /**
     * Saves the deployment files.
     * @return This object
     * @throws IOException If there is a problem writing the files
     */
    public Deployment save() throws IOException {
        if (this.isSupported()) {
            this.save0("terraform.tfvars", this.inputs());
            this.supportingFiles().forEach(this::save0);
        }
        return this;
    }

    /**
     * Saves the deployment files.
     * @param name The file name
     * @param content The file content
     */
    @SneakyThrows
    private void save0(final String name, final String content) {
        final File directory = new File(
            new File(Deployment.DIRECTORY),
            this.chromosome.identifier()
        );
        directory.mkdirs();
        final File file = new File(directory, name);
        file.getParentFile().mkdirs();
        Files.write(
            file.toPath(),
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
        final String id = this.chromosome.identifier();
        final File current = new File(String.format("%s/%s", Deployment.DIRECTORY, id));
        // TODO final String manifest = String.format("manifest.%s.yaml", this.variant);
        return CompletableFuture.supplyAsync(() -> {
            if (!this.isSupported()) {
                this.unsupported = true;
                Deployment.LOGGER.info("Flavor {} is unsupported", this.chromosome.formattedFlavor());
                return this;
            }
            try (TerraformClient terraform = new TerraformClient(current);
                 JMeterClient jmeter = new JMeterClient(current, Deployment.SCENARIOS);
                 KubernetesClient kubectl =
                     new KubernetesClient(current, Deployment.KUBECONFIG)) {
                if (terraform.version(5L, TimeUnit.SECONDS)
                    && terraform.init(5L, TimeUnit.MINUTES)
                    && terraform.apply(30L, TimeUnit.MINUTES)) {
                    Deployment.LOGGER.info("Cluster {} deployed successfully", id);
                    jmeter.version(5L, TimeUnit.SECONDS);
                    kubectl.version(5L, TimeUnit.SECONDS);
                    // TODO kubectl.apply(manifest, 5L, TimeUnit.MINUTES);
                    kubectl.waitUntilReady(5L, TimeUnit.MINUTES);
                    // TODO Add entry to hosts
                    // jmeter.run(this.scenario, 15L, TimeUnit.MINUTES);
                    // TODO this.score = RClient.computeScore();
                } else {
                    this.erroneous = true;
                    Deployment.LOGGER.info("{} There was an error running init/plan", id);
                }
                // Destroy deployed resources
                terraform.destroy(20L, TimeUnit.MINUTES);

                // FIXME delete this
                System.exit(0);
            } catch (final IOException | InterruptedException | TimeoutException exception) {
                Deployment.LOGGER.error("Unknown error: {}", exception.getMessage());
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
        if (this.erroneous || this.unsupported) {
            value = Double.MAX_VALUE;
        } else {
            // TODO Compute the performance score of this deployment
            value = 0.0;
        }
        return new Deployment.Score(this.erroneous, this.unsupported, value);
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
         * Whether the deployment could not be executed because it is unsupported.
         */
        boolean unsupported;

        /**
         * The deployment score. If there was an error, it is {@code Double.MAX_VALUE}.
         */
        double value;

    }

}
