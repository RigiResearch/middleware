package com.rigiresearch.middleware.experimentation.reification.fitness;

import com.rigiresearch.middleware.experimentation.sources.MetricSummary;
import lombok.Value;

/**
 * The result of executing a fitness function.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Value
public class FitnessResult {

    /**
     * The fitness value associated with this result.
     */
    double fitness;

    /**
     * A summary of the output metric.
     */
    MetricSummary summary;

}
