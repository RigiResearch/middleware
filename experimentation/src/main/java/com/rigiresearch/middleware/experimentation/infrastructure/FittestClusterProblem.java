package com.rigiresearch.middleware.experimentation.infrastructure;

import com.rigiresearch.middleware.experimentation.util.JMeterClient;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Codec;
import io.jenetics.engine.Codecs;
import io.jenetics.engine.Problem;
import io.jenetics.util.IntRange;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class representing the fittest cluster problem.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class FittestClusterProblem
    implements Problem<int[], IntegerGene, Double> {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(FittestClusterProblem.class);

    /**
     * The timeout in minutes.
     */
    private static final long TIMEOUT = 10L;

    /**
     * The number of worker nodes realizing the cluster.
     */
    private static final IntRange NODE = IntRange.of(1, 5);

    /**
     * The amount of memory.
     */
    private static final IntRange MEMORY = IntRange.of(1, 15);

    /**
     * The number of CPU cores.
     */
    private static final IntRange CPU = IntRange.of(1, 5);

    /**
     * Initial map capacity.
     */
    private static final int INITIAL_CAPACITY = 100;

    /**
     * The directory where results are written.
     */
    private static final String DIRECTORY = "deployments/data";

    /**
     * Memoized scores.
     */
    private final Map<String, Double> scores;

    /**
     * The file to store the fitness scores.
     */
    private final File file;

    /**
     * The name of the variant to deploy (included in the manifest name).
     */
    private final String variant;

    /**
     * The scenario to test.
     */
    private final JMeterClient.Scenario scenario;

    /**
     * Default constructor.
     * @throws IOException If there is a problem creating the results directory
     */
    public FittestClusterProblem(final String variant,
        JMeterClient.Scenario scenario) throws IOException {
        this.variant = variant;
        this.scenario = scenario;
        this.file = new File(FittestClusterProblem.DIRECTORY, "times.txt");
        this.scores = this.loadMemoizedResults();
    }

    /**
     * Loads previously memoized results.
     * @return A non-null, possibly empty map
     * @throws IOException If there is a problem creating the results directory
     */
    private Map<String, Double> loadMemoizedResults() throws IOException {
        this.file.getParentFile().mkdirs();
        final Map<String, Double> map =
            new HashMap<>(FittestClusterProblem.INITIAL_CAPACITY);
        if (this.file.exists()) {
            Files.readAllLines(this.file.toPath())
                .forEach(line -> {
                    final String[] parts = line.trim().split("=");
                    map.put(parts[0], Double.parseDouble(parts[1]));
                });
            FittestClusterProblem.LOGGER
                .info("Loaded {} previously memoized results", map.size());
        } else {
            FittestClusterProblem.LOGGER
                .debug("There are no memoized results to load");
        }
        return map;
    }

    @Override
    public Function<int[], Double> fitness() {
        return data -> {
            final CloudChromosome chromosome =
                new OracleCloudChromosome(data[2], data[1], data[0]);
            final String id = chromosome.identifier();
            final double score;
            if (this.scores.containsKey(id)) {
                score = this.scores.get(id);
            } else {
                try {
                    final Deployment.Score tmp =
                        new Deployment(chromosome, this.variant, this.scenario)
                            .save()
                            .deploy()
                            .get(FittestClusterProblem.TIMEOUT, TimeUnit.MINUTES)
                            .score();
                    score = tmp.getValue();
                    if (!tmp.isError()) {
                        this.memoize(id, score);
                        synchronized (this.scores) {
                            this.scores.put(id, tmp.getValue());
                        }
                    }
                } catch (final IOException | InterruptedException |
                    ExecutionException | TimeoutException exception) {
                    FittestClusterProblem.LOGGER
                        .error(exception.getLocalizedMessage(), exception);
                    throw new IllegalStateException(exception);
                }
            }
            return score;
        };
    }

    @Override
    public Codec<int[], IntegerGene> codec() {
        return Codecs.ofVector(
            FittestClusterProblem.NODE,
            FittestClusterProblem.MEMORY,
            FittestClusterProblem.CPU
        );
    }

    /**
     * Stores a partial result.
     * @param key The key computed from the chromosome data
     * @param score The computed fitness score
     */
    private void memoize(final String key, final double score) throws IOException {
        final String line = String.format("%s=%f", key, score);
        FittestClusterProblem.LOGGER.info(line);
        Files.write(
            this.file.toPath(),
            String.format("%s\n", line).getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE
        );
    }

}
