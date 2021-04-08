package com.rigiresearch.middleware.experimentation.util;

import com.rigiresearch.middleware.experimentation.util.monitoring.PrometheusDataCollector;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link PrometheusDataCollector}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class PrometheusDataCollectorTest {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PrometheusDataCollectorTest.class);

    @Test
    void testSampleServer() {
        final String period = "1m";
        final double quantile = 0.95;
        final PrometheusDataCollector collector = new PrometheusDataCollector(
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
        PrometheusDataCollectorTest.LOGGER.info("Collected value: {}", value);
    }

}
