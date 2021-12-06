package com.rigiresearch.middleware.experimentation.reification.fitness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A linear fitness function with a lower bound.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class LinearFitnessFunction implements FitnessFunction {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(LinearFitnessFunction.class);

    /**
     * A constant to compare doubles.
     */
    private static final double EPSILON = 0.00001;

    /**
     * The minimum acceptable value. Values less than the minimum will be Double.MAX_VALUE.
     */
    private final double min;

    /**
     * A unique name for the argument handled by this function.
     */
    private final String argument;

    @Override
    public double evaluate(final FitnessFunction.NamedArgument... arguments) {
        final Optional<FitnessFunction.NamedArgument> arg =
            FitnessFunction.argument(this.argument, arguments);
        if (arg.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Argument '%s' not found", this.argument)
            );
        }
        final double result;
        if (arg.get().getValue() - this.min >= LinearFitnessFunction.EPSILON) {
            // Make it negative to maximize the value
            result = -arg.get().getValue();
        } else {
            LinearFitnessFunction.LOGGER.warn(
                "Value {} is below the minimum boundary ({}) = Double.MIN_VALUE",
                arg.get().getValue(), this.min);
            result = Double.MIN_VALUE;
        }
        return result;
    }

    @Override
    public List<String> arguments() {
        return Collections.singletonList(this.argument);
    }

    @Override
    public String toString() {
        return String.format(
            "%s(argument: %s, min: %f)",
            this.getClass().getSimpleName(),
            this.argument,
            this.min
        );
    }

}
