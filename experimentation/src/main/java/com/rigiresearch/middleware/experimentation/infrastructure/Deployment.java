package com.rigiresearch.middleware.experimentation.infrastructure;

import com.google.common.collect.Lists;
import com.rigiresearch.middleware.experimentation.util.JMeterClient;
import com.rigiresearch.middleware.experimentation.util.KubernetesClient;
import com.rigiresearch.middleware.experimentation.util.RScriptClient;
import com.rigiresearch.middleware.experimentation.util.SoftwareVariant;
import com.rigiresearch.middleware.experimentation.util.TerraformClient;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

/**
 * A deployment coordinator.
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
     * URL of the metrics server to deploy.
     */
    private static final String METRICS_SERVER_URL =
        "https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.6.1/components.yaml";

    /**
     * The kube config file created after the terraform deployment.
     */
    private static final String KUBECONFIG = "./kubeconfig";

    /**
     * Pattern to recognize the output from the R score program.
     */
    private static final Pattern R_OUTPUT_REGEX = Pattern.compile("(\\[1\\]\\s+)(.*)\n?.*");

    /**
     * A random number generator.
     */
    private static final SecureRandom GENERATOR = new SecureRandom();

    /**
     * Service ports already being used.
     */
    private static final List<Integer> PORTS_IN_USE = new ArrayList<>();

    /**
     * The target cloud's actual deployment values.
     */
    private final CloudChromosome chromosome;

    /**
     * The variant to deploy.
     */
    private final SoftwareVariant variant;

    /**
     * The scenario to test.
     */
    private final JMeterClient.Scenario scenario;

    /**
     * The directory where results results are stored.
     */
    private final String directory;

    /**
     * A random and locally available port to proxy the Kubernetes service being tested.
     */
    private final int port;

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
     * @param variant The variant to deploy
     * @param scenario The scenario to test
     * @throws IOException If the template file is not found
     */
    public Deployment(final CloudChromosome chromosome, final SoftwareVariant variant,
        final JMeterClient.Scenario scenario)
        throws IOException {
        this.chromosome = chromosome;
        this.variant = variant;
        this.scenario = scenario;
        this.directory = String.format(
            "deployments/%s-%s",
            scenario.scenarioName(),
            variant.getName().variantName()
        );
        this.port = Deployment.randomPort();
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
            "terraform/fss.tf",
            "terraform/network.tf",
            "terraform/oke_cluster.tf",
            "terraform/oke_kube_config.tf",
            "terraform/outputs.tf",
            "terraform/provider.tf",
            "terraform/variables.tf",
            "scenarios/videos.csv",
            "score.R"
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
     * Render the chromosome data as jmeter scenario.
     * @return The contents of the inputs file
     * @throws IOException If there is a problem reading the inputs file
     */
    public String scenario() throws IOException {
        final ST template = new ST(
            new String(
                Objects.requireNonNull(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                            String.format(
                                "templates/scenarios/%s.jmx.st",
                                this.scenario.scenarioName()
                            )
                        )
                ).readAllBytes()
            ),
            '#',
            '#'
        );
        template.add("port", this.port);
        return template.render();
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
            this.save0(
                String.format("%s.jmx", this.scenario.scenarioName()),
                this.scenario()
            );
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
        final File fdirectory = new File(
            new File(this.directory),
            this.chromosome.identifier()
        );
        fdirectory.mkdirs();
        final File file = new File(fdirectory, name);
        // file.getParentFile().mkdirs();
        Files.write(
            // Flatten out the files by copying everything in the root directory
            new File(fdirectory, file.getName()).toPath(),
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
        final File current = new File(String.format("%s/%s", this.directory, id));
        final String manifest =
            String.format("manifest.%s.yaml", this.variant.getName().variantName());
        final KubernetesClient.PortForwardConfig config =
            new KubernetesClient.PortForwardConfig(
                this.variant.getService(),
                this.variant.getPort(),
                this.port,
                2L,
                TimeUnit.MINUTES
            );
        return CompletableFuture.supplyAsync(() -> {
            if (!this.isSupported()) {
                this.unsupported = true;
                Deployment.LOGGER.info("Flavor {} is unsupported", this.chromosome.formattedFlavor());
                return this;
            }
            try (TerraformClient terraform = new TerraformClient(current)) {
                try (JMeterClient jmeter = new JMeterClient(current);
                     KubernetesClient kubectl =
                         new KubernetesClient(current, Deployment.KUBECONFIG)) {
                    if (terraform.version(5L, TimeUnit.SECONDS)
                        && terraform.init(5L, TimeUnit.MINUTES)
                        && terraform.apply(1L, TimeUnit.HOURS)) {
                        Deployment.LOGGER.info("Cluster {} deployed successfully", id);
                        this.replaceOutputInManifest(
                            "#mount_target_ocid#",
                            terraform.output("FoggyKitchenMountTarget_ocid", 5L, TimeUnit.SECONDS)
                        );
                        // Deploy metrics server
                        kubectl.version(5L, TimeUnit.SECONDS);
                        kubectl.apply(Deployment.METRICS_SERVER_URL, "kube-system", 20L, TimeUnit.MINUTES);
                        kubectl.waitUntilReady("kube-system", 20L, TimeUnit.MINUTES);
                        // Deploy the software variant
                        kubectl.apply(manifest, "default", 10L, TimeUnit.MINUTES);
                        kubectl.waitUntilReady("default", 20L, TimeUnit.MINUTES);
                        // Expose the service
                        kubectl.portForward(config, 1L, TimeUnit.MINUTES);
                        // Run tests
                        jmeter.version(5L, TimeUnit.SECONDS);
                        jmeter.run(this.scenario, this.variant, 1L, TimeUnit.HOURS);
                    } else {
                        this.erroneous = true;
                        Deployment.LOGGER.info("{} There was an error running init/plan", id);
                    }
                } catch (final IOException | InterruptedException | TimeoutException exception) {
                    Deployment.LOGGER.error("Unknown error: {}", exception.getMessage());
                    throw new IllegalStateException(exception);
                } finally {
                    // Destroy deployed resources
                    terraform.destroy(30L, TimeUnit.MINUTES);
                }
            } catch (final IOException | InterruptedException | TimeoutException exception) {
                Deployment.LOGGER.error("Terraform error: {}", exception.getMessage());
                throw new IllegalStateException(exception);
            }
            return this;
        });
    }

    /**
     * Replace a template variable in the manifest file.
     * @param variable The template variable
     * @param output The new content
     * @throws IOException If there is a problem reading or writing the file
     */
    private final void replaceOutputInManifest(final String variable,
        final String output) throws IOException {
        final String name =
            String.format("manifest.%s.yaml", this.variant.getName().variantName());
        final File deployment = new File(this.directory, this.chromosome.identifier());
        final File manifest = new File(deployment, name);
        final String original = new String(
            Files.readAllBytes(manifest.toPath()),
            StandardCharsets.UTF_8
        );
        final String content = original.replaceAll(variable, output.trim());
        Files.write(manifest.toPath(), content.getBytes(StandardCharsets.UTF_8));
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
            final File deployment = new File(this.directory, this.chromosome.identifier());
            final String file = String.format(
                "%s-%s.csv",
                this.scenario.scenarioName(),
                this.variant.getName().variantName()
            );
            try {
                final RScriptClient R = new RScriptClient(deployment);
                final String output = R.output(2L, TimeUnit.MINUTES, "score.R", file);
                final Matcher matcher = Deployment.R_OUTPUT_REGEX.matcher(output);
                if (matcher.matches()) {
                    value = Double.parseDouble(matcher.group(2));
                } else {
                    value = Double.MAX_VALUE;
                }
            } catch (final IOException | InterruptedException | TimeoutException exception) {
                Deployment.LOGGER.error("Error running R script", exception);
                throw new IllegalStateException(exception);
            }
            Deployment.LOGGER.info("Score ({}) = {}", this.chromosome.identifier(), value);
        }
        return new Deployment.Score(this.erroneous, this.unsupported, value);
    }

    /**
     * Generates a random port.
     * @return A random integer between 10k and 60k, that is not currently in use.
     */
    private static int randomPort() {
        int port;
        final int min = 10_000;
        final int max = 60_000;
        synchronized (Deployment.PORTS_IN_USE) {
            do {
                port = min + Deployment.GENERATOR.nextInt(max - min + 1);
            } while (Deployment.PORTS_IN_USE.contains(port));
            Deployment.PORTS_IN_USE.add(port);
        }
        return port;
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
