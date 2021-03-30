package com.rigiresearch.middleware.experimentation.infrastructure;

import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.RandomRegistry;
import java.security.SecureRandom;
import java.util.concurrent.Executor;

/**
 * The experiment setup.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class FittestClusterExperiment {

    /**
     * The Jenetics engine.
     */
    private final Engine<IntegerGene, Double> engine;

    /**
     * The maximum number of iterations allowed.
     */
    private final long generations;

    /**
     * Default constructor.
     * @param generations The maximum number of iterations allowed
     * @param executor The executor service to use
     */
    public FittestClusterExperiment(final long generations, final Executor executor) {
        this.generations = generations;
        this.engine = Engine.builder(new FittestClusterProblem())
            .minimizing()
            // Default value
            // .alterers(new SinglePointCrossover<>(0.2), new Mutator<>(0.15))
            // .offspringFraction(0.6)
            // .offspringSelector(new TruncationSelector<>(3))
            // .survivorsSelector(new TruncationSelector<>(3))
            // A single thread to create only one cluster at a time
            .executor(executor)
            // .maximalPhenotypeAge(70L)
            // .populationSize(50)
            .build();
    }

    /**
     * Runs this experiment.
     * @param seed A seed for the random number generator
     * @return The result of the execution
     */
    public ExperimentResult run(final byte[] seed) {
        RandomRegistry.random(new SecureRandom(seed));
        final EvolutionStatistics<Double, DoubleMomentStatistics> statistics =
            EvolutionStatistics.ofNumber();
        final EvolutionResult<IntegerGene, Double> result = this.engine.stream()
            .limit(this.generations)
            .peek(statistics)
            .collect(EvolutionResult.toBestEvolutionResult());
        return new ExperimentResult(result, statistics);
    }

}
