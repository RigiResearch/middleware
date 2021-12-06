package com.rigiresearch.middleware.experimentation.reification.fitness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a cubic fitness function to reward minimizing certain variable.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class CubicFitnessFunction implements FitnessFunction {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(CubicFitnessFunction.class);

    /**
     * The lower bound in the x axis.
     */
    private final BigDecimal a;

    /**
     * A value between a and c;
     */
    private final BigDecimal b;

    /**
     * The upper bound in the x axis.
     */
    private final BigDecimal c;

    /**
     * A unique name for the argument handled by this function.
     */
    private final String argument;

    /**
     * Default constructor.
     * @param a The lower bound in the x axis.
     * @param b A value between a and c;
     * @param c The upper bound in the x axis.
     * @param argument A unique name for the argument handled by this function
     */
    public CubicFitnessFunction(final double a, final double b,
        final double c, final String argument) {
        this.a = new BigDecimal(a);
        this.b = new BigDecimal(b);
        this.c = new BigDecimal(c);
        this.argument = argument;
    }

    /**
     * Evaluates the function {@code -10(x-b)^3}, where {@code a<=x<=c} and
     * {@code a<=b<=c}. This function translates {@code -X^3} to the right and
     * stretches it so that {@code f(x)} is a positive number and is greater
     * when {@code x} tends to {@code a}, and is a negative number and smaller
     * when it tends to {@code b}.
     *
     * <p>When {@code x<a} or {@code x>c}, this function returns negative or
     * positive infinity, respectively.</p>
     *
     * <p>Visit https://www.wolframalpha.com/input/?i=plot+-10%28x-18%29%5E3+from+0+to+36
     * to see a plot of this function.</p>
     *
     * @param arg One value on the x axis to evaluate the function
     * @return A negative or positive number, or 0 when x = b
     */
    public double evaluateNonNormalized(final double arg) {
        final BigDecimal x = BigDecimal.valueOf(arg);
        final double y;
        if (CubicFitnessFunction.lessThanOrEqual(this.a, x) &&
            CubicFitnessFunction.lessThanOrEqual(x, this.c)) {
            y = -10.0 * StrictMath.pow(x.subtract(this.b).doubleValue(), 3.0);
        } else if (x.compareTo(this.a) < 0) {
            y = Double.NEGATIVE_INFINITY;
            CubicFitnessFunction.LOGGER
                .warn("Value {} is below the minimum boundary ({}) = -Inf", x, this.a);
        } else {
            y = Double.POSITIVE_INFINITY;
            CubicFitnessFunction.LOGGER
                .warn("Value {} is above the maximum boundary ({}) = +Inf", x, this.c);
        }
        return y;
    }

    /**
     * Same as {@link #evaluateNonNormalized(double)} (double[])} but normalized.
     * Since {@code b} represents no change with respect to the initial plan
     * (i.e., {@code x<b} is improvement and {@code x>b} is decline),
     * {@code normalized(f(x))=0} when {@code x=b}. Therefore, this function is
     * defined by parts:
     * <pre>
     *     f(x) = {
     *       normalized(f(x)) between (0, 1], when a<=x<=b
     *       0, when x=b
     *       normalized(f(x)) between [-1, 0), when b<=x<=c
     *     }
     * </pre>
     * <p>When {@code x<a} or {@code x>b}, this function returns -1.</p>
     * @param arguments One value on the x axis to evaluate the function
     * @return A number between -1 and 1
     */
    @Override
    public double evaluate(final FitnessFunction.NamedArgument... arguments) {
        final Optional<FitnessFunction.NamedArgument> arg =
            FitnessFunction.argument(this.argument, arguments);
        if (arg.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Argument '%s' not found", this.argument)
            );
        }
        final BigDecimal x = BigDecimal.valueOf(arg.get().getValue());
        final double y = this.evaluateNonNormalized(arg.get().getValue());
        final double normalized;
        if (CubicFitnessFunction.lessThanOrEqual(this.a, x) &&
            CubicFitnessFunction.lessThanOrEqual(x, this.b)) {
            final double min = this.evaluateNonNormalized(this.a.doubleValue());
            final double max = this.evaluateNonNormalized(this.b.doubleValue());
            // Switch min and max so that when x=a, y=1 and when x=b, y=0
            normalized = FitnessFunction.normalizeInRange(y, max, min, 0.0, 1.0);
        } else if (CubicFitnessFunction.lessThanOrEqual(this.b, x) &&
            CubicFitnessFunction.lessThanOrEqual(x, this.c)) {
            final double min = this.evaluateNonNormalized(this.b.doubleValue());
            final double max = this.evaluateNonNormalized(this.c.doubleValue());
            // Switch min and max so that when x=b, y=0 and when x=c, y=-1
            normalized = FitnessFunction.normalizeInRange(y, max, min, -1.0, 0.0);
        } else {
            normalized = -1.0;
        }
        return normalized;
    }

    @Override
    public List<String> arguments() {
        return Collections.singletonList(this.argument);
    }

    /**
     * The less-than-or-equal relational operator.
     * @param first The first argument of the relational operator
     * @param second The second argument of the relational operator
     * @return first <= second
     */
    private static boolean lessThanOrEqual(final BigDecimal first,
        final BigDecimal second) {
        final int result = first.compareTo(second);
        return result < 0 || result == 0;
    }

    @Override
    public String toString() {
        return String.format(
            "%s(argument: %s, a: %f, b: %f, c: %f)",
            this.getClass().getSimpleName(),
            this.argument,
            this.a,
            this.b,
            this.c
        );
    }

}
