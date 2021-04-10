package com.rigiresearch.middleware.experimentation.sources;

/**
 * A metric source.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface MetricStore {

    /**
     * Collects a summary of a given metric.
     * @param metric The metric name
     * @return A non-null object
     */
    MetricSummary metricSummary(String metric);

}
