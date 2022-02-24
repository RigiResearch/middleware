package com.rigiresearch.middleware.experimentation.infrastructure;

import com.rigiresearch.middleware.experimentation.util.KubernetesClient;
import com.rigiresearch.middleware.experimentation.util.TerraformClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
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
     * The kube config file created after the terraform deployment.
     */
    private static final String KUBECONFIG = "./rke2.yaml";

    /**
     * Compute Canada's actual deployment values.
     */
    private final ComputeCanadaChromosome chromosome;

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
     * @throws IOException If the template file is not found
     */
    public Deployment(final ComputeCanadaChromosome chromosome)
        throws IOException {
        this.chromosome = chromosome;
    }

    /**
     * Whether this deployment is supported by the target cloud.
     * @return {@code True} if the deployment can be executed, {@code False} otherwise
     */
    public boolean isSupported() {
        return this.chromosome.isSupported();
    }

    /**
     * Render the chromosome data as the main template file.
     * @return The contents of the main file
     * @throws IOException If there is an error reading the specification file
     */
    public String main() throws IOException {
        return new String(
            Objects.requireNonNull(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("templates/main.tf")
            ).readAllBytes()
        );
    }

    /**
     * Reads supporting files.
     * @return The contents of supporting files and their paths
     * @throws IOException If there is an error reading the files
     */
    public Map<String, String> supportingFiles() throws IOException {
        final String manifest = "manifests/manifest.yaml";
        final String config = "rke2_config.yaml";
        final Map<String, String> files = new HashMap<>(2);
        files.put(
            manifest,
            new String(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                            String.format("templates/%s", manifest)
                        )
                ).readAllBytes()
            )
        );
        files.put(
            config,
            new String(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                            String.format("templates/%s", config)
                        )
                ).readAllBytes()
            )
        );
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
        template.add("flavor", this.chromosome.flavor());
        return template.render();
    }

    /**
     * Saves the deployment files.
     * @return This object
     * @throws IOException If there is a problem writing the files
     */
    public Deployment save() throws IOException {
        if (this.isSupported()) {
            this.save0("main.tf", this.main());
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
        return CompletableFuture.supplyAsync(() -> {
            if (!this.isSupported()) {
                this.unsupported = true;
                Deployment.LOGGER.info("Flavor {} is unsupported", this.chromosome.formattedFlavor());
                return this;
            }
            try (TerraformClient terraform = new TerraformClient(current);
                 KubernetesClient kubectl =
                     new KubernetesClient(current, Deployment.KUBECONFIG)) {
                if (terraform.version(5L, TimeUnit.SECONDS)
                    && terraform.init(2L, TimeUnit.MINUTES)
                    && terraform.apply(15L, TimeUnit.MINUTES)) {
                    Deployment.LOGGER.info("Cluster {} deployed successfully", id);
                    kubectl.version(5L, TimeUnit.SECONDS);
                    // Sleep while kubernetes launches the pods
                    Thread.sleep(5000L);
                    kubectl.waitUntilReady(5L, TimeUnit.MINUTES);
                    // FIXME Remove hardcoded service values
                    final KubernetesClient.PortForwardConfig config =
                        new KubernetesClient.PortForwardConfig(
                            "hello-kubernetes-first", 80, 8080, 2L, TimeUnit.MINUTES
                        );
                    kubectl.portForward(config, 10L, TimeUnit.SECONDS);
                    // TODO Run performance tests per execution scenario
                } else {
                    this.erroneous = true;
                    Deployment.LOGGER.info("{} There was an error running init/plan", id);
                }
                // Destroy deployed resources
                terraform.destroy(10L, TimeUnit.MINUTES);

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
