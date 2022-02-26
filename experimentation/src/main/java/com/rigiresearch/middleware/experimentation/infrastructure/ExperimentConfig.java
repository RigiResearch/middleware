package com.rigiresearch.middleware.experimentation.infrastructure;

import com.rigiresearch.middleware.experimentation.util.JMeterClient;
import com.rigiresearch.middleware.experimentation.util.SoftwareVariant;
import java.util.concurrent.Executor;
import lombok.Value;

/**
 * Configuration values to instantiate a fittest cluster experiment.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Value
public class ExperimentConfig {

    /**
     * The maximum number of iterations allowed.
     */
    long generations;

    /**
     * The initial population size.
     */
    int population;

    /**
     * The number of subsequent steady fitness values to stop.
     */
    int stop;

    /**
     * The number of (best) results to collect.
     */
    int results;

    /**
     * The chromosome crossover probability.
     */
    double crossover;

    /**
     * The chromosome mutation probability.
     */
    double mutation;

    /**
     * The executor service to use.
     */
    Executor executor;

    /**
     * The name of the variant to deploy (included in the manifest name).
     */
    SoftwareVariant variant;

    /**
     * The scenario to test.
     */
    JMeterClient.Scenario scenario;

}
