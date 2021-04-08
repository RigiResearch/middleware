package com.rigiresearch.middleware.experimentation.util.monitoring;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * A metric data collector.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class PrometheusDataCollector {

    /**
     * An HTTP client.
     */
    private final HttpClient client;

    /**
     * The prometheus host.
     */
    private final String host;

    /**
     * The time period to use in the queries.
     */
    private final String period;

    /**
     * The quantile to use in the queries.
     */
    private final double quantile;

    /**
     * Default constructor.
     * @param host The prometheus host
     * @param period The time period to use in the queries
     * @param quantile The quantile to use in the queries
     */
    public PrometheusDataCollector(final String host, final String period,
        final double quantile) {
        this.client = HttpClient.newHttpClient();
        this.host = host;
        this.period = period;
        this.quantile = quantile;
    }

    /**
     * Collects data to summarize a given metric.
     * @param metric The metric summary
     * @return A non-null object
     */
    public PrometheusMetricSummary metricSummary(final String metric) {
        // TODO Finish setting up the metric summary when the actual metrics are available
        return PrometheusMetricSummary.builder()
            .min(this.collectValue(
                String.format(
                    "histogram_quantile(%f, sum(rate(%s[%s])) by (le))",
                    this.quantile,
                    metric,
                    this.period
                )
            ))
            .build();
    }

    /**
     * Collects a specific value for a given query.
     * @param query The query to execute
     * @return The corresponding value
     */
    public double collectValue(final String query) {
        final HttpRequest request = HttpRequest.newBuilder()
            .timeout(Duration.ofMinutes(1L))
            .uri(
                URI.create(
                    String.format(
                        "%s/api/v1/query?query=%s",
                        this.host,
                        URLEncoder.encode(query, StandardCharsets.UTF_8)
                    )
                )
            ).build();
        return JsonPath.read(
            this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .join(),
            "$.data.result[0].value[0]"
        );
    }

}
