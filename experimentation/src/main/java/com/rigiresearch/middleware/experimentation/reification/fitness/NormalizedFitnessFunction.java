package com.rigiresearch.middleware.experimentation.reification.fitness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a normalized fitness function to reward minimizing certain variable.
 *
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class NormalizedFitnessFunction implements FitnessFunction {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(NormalizedFitnessFunction.class);

    /**
     * The minimum acceptable value.
     */
    private final double min;

    /**
     * The maximum acceptable value.
     */
    private final double max;

    /**
     * A unique name for the argument handled by this function.
     */
    private final String argument;

    /**
     * Evaluates the function {@code -x+1}, where {@code 0<=x<=max}. f(x) is
     * greater when {@code x} tends to 0, and is smaller when it tends to
     * {@code max}.
     * <p>Visit https://www.wolframalpha.com/input/?i=plot+-x+%2B+1+from+0+to+1
     * to see a plot of this function.</p>
     *
     * @param arguments The arguments passed to this function
     * @return a double value between -1.0 and 1.0
     */
    @Override
    public double evaluate(final FitnessFunction.NamedArgument... arguments) {
        final Optional<FitnessFunction.NamedArgument> arg =
                FitnessFunction.argument(this.argument, arguments);
        this.checkArgument(arg);
        final double result;
        if (arg.get().getValue() < this.min) {
            result = Double.MIN_VALUE;
            NormalizedFitnessFunction.LOGGER.warn(
                "Value {} is below the minimum boundary ({})", arg.get().getValue(), this.min);
        } else if (arg.get().getValue() > this.max) {
            result = -1;
            NormalizedFitnessFunction.LOGGER.warn(
                "Value {} is above the maximum boundary ({})", arg.get().getValue(), this.max);
        } else {
            result = FitnessFunction.normalizeInRange(
                    arg.get().getValue(),
                    this.max,
                    this.min,
                    -1.0,
                    1.0
            );
        }
        return result;
    }

    @Override
    public List<String> arguments() {
        return Collections.singletonList(this.argument);
    }

    /**
     * Checks preconditions on the arguments.
     *
     * @param arg The argument
     */
    private void checkArgument(final Optional<FitnessFunction.NamedArgument> arg) {
        if (arg.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Argument '%s' not found", this.argument)
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%s(argument: %s, min: %f, max: %f)",
                this.getClass().getSimpleName(),
                this.argument,
                this.min,
                this.max
        );
    }

}
