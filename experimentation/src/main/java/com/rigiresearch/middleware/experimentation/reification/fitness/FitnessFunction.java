package com.rigiresearch.middleware.experimentation.reification.fitness;

import java.util.List;
import java.util.Optional;
import lombok.Value;

/**
 * A fitness function to either minimize or maximize a value.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface FitnessFunction {

    /**
     * Evaluate this function.
     * @param arguments The arguments passed to this function
     * @return A positive or negative number, including 0
     */
    double evaluate(FitnessFunction.NamedArgument... arguments);

    /**
     * The names of the arguments accepted by this function.
     * @return A list of names
     */
    List<String> arguments();

    /**
     * Finds an argument by its name.
     * @param name The argument name
     * @param arguments The function arguments
     * @return The argument, if there is a match
     */
    static Optional<FitnessFunction.NamedArgument> argument(final String name,
        final FitnessFunction.NamedArgument[] arguments) {
        Optional<FitnessFunction.NamedArgument> optional = Optional.empty();
        for (final FitnessFunction.NamedArgument arg : arguments) {
            if (arg.getName().equals(name)) {
                optional = Optional.of(arg);
                break;
            }
        }
        return optional;
    }

    /**
     * Calculates a value between 0 and 1, given the precondition that value
     * is between min and max.
     * @param value The value to scale
     * @param min The minimum value in the domain
     * @param max The maximum value in the domain
     * @return A double between 0.0 and 1.0
     */
    static double normalize(final double value, final double min,
        final double max) {
        return FitnessFunction.normalizeInRange(value, min, max, 0.0, 1.0);
    }

    /**
     * Calculates a value between {@code a} and {@code b}, given the
     * precondition that value is between {@code min} and {@code max}. {@code a}
     * means {@code value = max}, and {@code b} means {@code value = min}.
     *
     * <p>From https://stats.stackexchange.com/a/178629</p>
     *
     * @param value The value to scale
     * @param min The minimum value in the domain
     * @param max The maximum value in the domain
     * @param a The lower bound of the range
     * @param b The upper bound of the range
     * @return A double between {@code a} and {@code b}
     */
    static double normalizeInRange(final double value, final double min,
        final double max, final double a, final double b) {
        return (b - a) * ((value-min)/(max-min)) + a;
    }

    @Value
    class NamedArgument {

        /**
         * This argument's name.
         */
        String name;

        /**
         * This argument's value.
         */
        Double value;

    }

}
