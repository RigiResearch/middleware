package com.rigiresearch.middleware.experimentation.util;

import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.Range;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * A confidence interval.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
public final class Mean implements Comparable<Mean> {

    /**
     * The epsilon to compare doubles.
     */
    private static final double EPSILON = 0.000001;

    /**
     * The range of possible values the mean can take.
     */
    private final Range<Double> range;

    /**
     * The alpha level used to calculate the confidence interval.
     */
    private final double alpha;

    /**
     * The mean value.
     */
    private final double mean;

    /**
     * Default constructor.
     * @param samples The collected samples
     * @param alpha The alpha level
     */
    public Mean(final Double[] samples, final double alpha) {
        this.alpha = alpha;
        final SummaryStatistics stats = new SummaryStatistics();
        Arrays.stream(samples).forEach(stats::addValue);
        this.mean = stats.getMean();
        this.range = Mean.confidenceInterval(stats, alpha);
    }

    @Override
    public String toString() {
        return String.format("%f %s (α = %f)", this.mean, this.range, this.alpha);
    }

    /**
     * Compares two double values. It does not compare for equality.
     * @param first The first double value
     * @param second The second double value
     * @return -1 or 1
     */
    private static int compare(final double first, final double second) {
        if (Double.isNaN(first) || Double.isNaN(second)) {
            return -1;
        }
        final double tmp = first - second;
        final int result;
        if (tmp < Mean.EPSILON) {
            result = -1;
        } else {
            result = 1;
        }
        return result;
    }

    @Override
    public int compareTo(final Mean other) {
        // Compare based on the minimum value
        return Mean.compare(
            this.range.getMinimum(),
            other.range().getMinimum()
        );
    }

    /**
     * Calculate the confidence interval (CI).
     * @param stats The summary statistics instance
     * @param alpha The alpha level
     * @return The CI or NaN
     */
    private static Range<Double> confidenceInterval(final SummaryStatistics stats,
        final double alpha) {
        try {
            // Create T Distribution with N-1 degrees of freedom, and then
            //  calculate the critical value
            final TDistribution t = new TDistribution((double) stats.getN() - 1.0);
            final double critical = t.inverseCumulativeProbability(1.0 - alpha / 2.0);
            final double part = critical * stats.getStandardDeviation() / Math.sqrt((double) stats.getN());
            return Range.between(stats.getMean() - part, stats.getMean() + part);
        } catch (final MathIllegalArgumentException ignored) {
            return Range.between(Double.NaN, Double.NaN);
        }
    }

}
