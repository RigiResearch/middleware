package com.rigiresearch.middleware.experimentation.util.monitoring;

import lombok.Builder;
import lombok.Value;

/**
 * Descriptive summary of a particular metric.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Builder
@Value
public class PrometheusMetricSummary {

    /**
     * The minimum value within the collected values.
     */
    double min;

    /**
     * The maximum value within the collected values.
     */
    double max;

    /**
     * The population standard deviation of the collected values.
     */
    double std;

    /**
     * The population standard variance of the collected values.
     */
    double var;

    /**
     * The mean of the collected values.
     */
    double mean;

    /**
     * The number of collected values.
     */
    int count;

    /**
     * The metric being summarized.
     */
    String name;

}
