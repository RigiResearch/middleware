package com.rigiresearch.middleware.experimentation;

import com.rigiresearch.middleware.experimentation.infrastructure.ExperimentConfig;
import com.rigiresearch.middleware.experimentation.infrastructure.ExperimentResult;
import com.rigiresearch.middleware.experimentation.infrastructure.FittestClusterExperiment;
import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionResult;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Application.class);

    /**
     * The size of the thread pool size.
     */
    private static final int THREADS = 5;

    /**
     * The number of generations.
     */
    private static final long GENERATIONS = 10L;

    /**
     * The size of the initial population.
     */
    private static final int POPULATION = 10;

    /**
     * The number of subsequent steady fitness values to stop.
     */
    private static final int STEADY_GENERATIONS = 5;

    /**
     * The number of (best) results to collect.
     */
    private static final int NUM_RESULTS = 50;

    /**
     * The chromosome crossover probability.
     */
    private static final double CROSSOVER_PROBABILITY = 0.5;

    /**
     * The chromosome mutation probability.
     */
    private static final double MUTATION_PROBABILITY = 0.05;

    /**
     * Default constructor.
     */
    private Application() {
        // Nothing to do here
    }

    /**
     * Run this application.
     */
    public void run() {
        final ExecutorService executor = Executors.newFixedThreadPool(Application.THREADS);
        final ExperimentResult result = new FittestClusterExperiment(
            new ExperimentConfig(
                Application.GENERATIONS,
                Application.POPULATION,
                Application.STEADY_GENERATIONS,
                Application.NUM_RESULTS,
                Application.CROSSOVER_PROBABILITY,
                Application.MUTATION_PROBABILITY,
                executor
            )
        ).run("SEED".getBytes());
        executor.shutdown();
        final Optional<EvolutionResult<IntegerGene, Double>> phenotype = result.getResult()
            .stream()
            .findFirst();
        if (phenotype.isPresent()) {
            Application.LOGGER.info("\n{}", result.getStatistics());
            Application.LOGGER.info(
                "Fitness: {}, Genotype: {}",
                phenotype.get().bestPhenotype().fitness(),
                phenotype.get().bestPhenotype().genotype()
            );
        }
    }

    /**
     * The main entry point.
     * @param args The application arguments
     */
    public static void main(final String... args) {
        new Application().run();
    }

}
