package com.rigiresearch.middleware.experimentation.sources;

/**
 * Descriptive summary of a particular metric.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface MetricSummary {

    /**
     * The minimum value within the collected values.
     */
    double getMin();

    /**
     * The maximum value within the collected values.
     */
    double getMax();

    /**
     * The population standard deviation of the collected values.
     */
    double getStd();

    /**
     * The population standard variance of the collected values.
     */
    double getVar();

    /**
     * The mean of the collected values.
     */
    double getMean();

    /**
     * The number of collected values.
     */
    int getCount();

    /**
     * The metric being summarized.
     */
    String getName();

}
