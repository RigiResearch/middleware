package com.rigiresearch.middleware.experimentation.util;

import com.rigiresearch.middleware.experimentation.sources.prometheus.PrometheusMetricStore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link PrometheusMetricStore}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class PrometheusMetricStoreTest {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PrometheusMetricStoreTest.class);

    @Test
    void testSampleServer() {
        final String period = "1m";
        final double quantile = 0.95;
        final PrometheusMetricStore collector = new PrometheusMetricStore(
            "http://demo.robustperception.io:9090",
            period,
            quantile
        );
        final String metric = "prometheus_http_request_duration_seconds_bucket";
        final String query = String.format(
            "histogram_quantile(%f, sum(rate(%s[%s])) by (le))",
            quantile,
            metric,
            period
        );
        final double value = collector.collectValue(query);
        PrometheusMetricStoreTest.LOGGER.info("Collected value: {}", value);
    }

}
