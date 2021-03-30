package com.rigiresearch.middleware.experimentation;

import com.rigiresearch.middleware.experimentation.infrastructure.ExperimentResult;
import com.rigiresearch.middleware.experimentation.infrastructure.FittestClusterExperiment;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import java.io.IOException;
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
     * Default constructor.
     */
    private Application() {
        // Nothing to do here
    }

    /**
     * Run this application.
     * @param generations The maximum number of iterations to perform
     */
    public void run(final long generations) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ExperimentResult result = new FittestClusterExperiment(generations, executor)
            .run("HW".getBytes());
        executor.shutdown();
        final Phenotype<IntegerGene, Double> phenotype = result.getResult().bestPhenotype();
        Application.LOGGER.info("\n{}", result.getStatistics());
        Application.LOGGER.info(
            "Fitness: {}, Genotype: {}",
            phenotype.fitness(),
            phenotype.genotype()
        );
    }

    /**
     * The main entry point.
     * @param args The application arguments
     * @throws HclParsingException If there is a problem interpreting the template
     * @throws IOException If the template file is not found
     */
    public static void main(final String... args) throws HclParsingException, IOException {
        final long generations;
        if (args.length == 1) {
            generations = Long.parseLong(args[0]);
        } else {
            generations = 5L;
        }
        new Application().run(generations);
    }

}
