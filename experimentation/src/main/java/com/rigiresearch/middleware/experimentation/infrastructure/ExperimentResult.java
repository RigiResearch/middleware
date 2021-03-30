package com.rigiresearch.middleware.experimentation.infrastructure;

import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.ISeq;
import lombok.Value;

/**
 * The result of an experiment.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Value
public class ExperimentResult {

    /**
     * The evolution result.
     */
    ISeq<EvolutionResult<IntegerGene, Double>> result;

    /**
     * Summary statistics drawn from the evolution process.
     */
    EvolutionStatistics<Double, DoubleMomentStatistics> statistics;

}
