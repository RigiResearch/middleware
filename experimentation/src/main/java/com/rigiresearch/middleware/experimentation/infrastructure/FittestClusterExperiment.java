package com.rigiresearch.middleware.experimentation.infrastructure;

import io.jenetics.IntegerGene;
import io.jenetics.Mutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.stat.MinMax;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;
import java.security.SecureRandom;

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
     * The configuration parameters.
     */
    private final ExperimentConfig config;
    /**
     * Default constructor.
     * @param config Configuration parameters
     */
    public FittestClusterExperiment(final ExperimentConfig config) {
        this.config = config;
        this.engine = Engine.builder(new FittestClusterProblem())
            .minimizing()
            // Default value
            .alterers(
                new SinglePointCrossover<>(this.config.getCrossover()),
                new Mutator<>(this.config.getMutation())
            )
            .executor(config.getExecutor())
            .populationSize(config.getPopulation())
            .minimizing()
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
        final ISeq<EvolutionResult<IntegerGene, Double>> result = this.engine.stream()
            .limit(Limits.bySteadyFitness(this.config.getStop()))
            .limit(Limits.byFixedGeneration(this.config.getGenerations()))
            .peek(statistics)
            .flatMap(MinMax.toStrictlyIncreasing())
            .collect(ISeq.toISeq(this.config.getResults()));
        // .collect(EvolutionResult.toBestEvolutionResult());
        return new ExperimentResult(result, statistics);
    }

}
