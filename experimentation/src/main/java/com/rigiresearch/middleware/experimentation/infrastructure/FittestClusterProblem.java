package com.rigiresearch.middleware.experimentation.infrastructure;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
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
     * The number of virtual nodes realizing the cluster, including 2, 4, and 6.
     */
    private static final IntRange NODE = IntRange.of(1, 3);

    /**
     * The amount of memory in Gb.
     */
    private static final IntRange MEMORY = IntRange.of(1, 5);

    /**
     * The number of CPU cores, including 2, 4 or 6 cores.
     */
    private static final IntRange CPU = IntRange.of(1, 3);

    /**
     * The network speed, including 10Mb, 100Mb and 1Gb.
     */
    private static final IntRange NETWORK = IntRange.of(1, 3);

    /**
     * Initial map capacity.
     */
    private static final int INITIAL_CAPACITY = 100;

    /**
     * The directory where results arre written.
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
     * Default constructor.
     * @throws IOException If there is a problem creating the results directory
     */
    public FittestClusterProblem() throws IOException {
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
                .info("Load {} previously memoized results", map.size());
        } else {
            FittestClusterProblem.LOGGER
                .debug("There are no memoized results to load");
        }
        return map;
    }

    @Override
    public Function<int[], Double> fitness() {
        return data -> {
            final String id =
                String.format("%d-%d-%d-%d", data[0], data[1], data[2], data[3]);
            this.scores.computeIfAbsent(id, key -> {
                final Deployment.Score score;
                try {
                    score = new Deployment(data)
                        .save()
                        .deploy()
                        .get(FittestClusterProblem.TIMEOUT, TimeUnit.MINUTES)
                        .score();
                    this.memoize(key, score.getValue());
                } catch (final IOException | HclParsingException |
                    InterruptedException | ExecutionException |
                    TimeoutException exception) {
                    FittestClusterProblem.LOGGER
                        .error(exception.getLocalizedMessage(), exception);
                    throw new IllegalStateException(exception);
                }
                return score.getValue();
            });
            return this.scores.get(id);
        };
    }

    @Override
    public Codec<int[], IntegerGene> codec() {
        return Codecs.ofVector(
            FittestClusterProblem.NODE,
            FittestClusterProblem.MEMORY,
            FittestClusterProblem.CPU,
            FittestClusterProblem.NETWORK
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
