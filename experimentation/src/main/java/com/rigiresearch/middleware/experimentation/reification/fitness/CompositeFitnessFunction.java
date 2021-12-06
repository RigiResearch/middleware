package com.rigiresearch.middleware.experimentation.reification.fitness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Defines a composite fitness function with percentage weights.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class CompositeFitnessFunction implements FitnessFunction {

    /**
     * Initial capacity based on the expected number of fitness functions.
     */
    private static final int INITIAL_CAPACITY = 5;

    /**
     * A small value to compare double values.
     */
    private static final double EPSILON = 0.000001;

    /**
     * A list of pairs function-weight to compute the overall weight.
     */
    private final List<CompositeFitnessFunction.Pair> pairs;

    /**
     * Default constructor.
     */
    public CompositeFitnessFunction() {
        this.pairs = new ArrayList<>(CompositeFitnessFunction.INITIAL_CAPACITY);
    }

    /**
     * Adds a fitness function to this composite function.
     * @param function The fitness function
     * @param weight The fitness function's weight on the final fitness score
     * @return This
     */
   public CompositeFitnessFunction withFunction(final FitnessFunction function,
       final double weight) {
        this.pairs.add(new CompositeFitnessFunction.Pair(function, weight));
        return this;
   }

    /**
     * Validates that this function has been built correctly. That is, the weights
     * sum 1, and all argument types are handled by only one function.
     * @return This
     */
    public CompositeFitnessFunction validate() {
        final List<String> arguments = this.arguments();
        if (arguments.size() != new HashSet<>(arguments).size()) {
            throw new IllegalStateException(
                "There are at least two functions handling the same argument name"
            );
        }
        final double sum = this.pairs.stream()
            .map(CompositeFitnessFunction.Pair::getWeight)
            .mapToDouble(value -> value)
            .sum();
        if (Math.abs(1.0 - sum) > CompositeFitnessFunction.EPSILON) {
            throw new IllegalStateException(
                String.format("The weights must sum 1.0. Current value is %f", sum)
            );
        }
        return this;
    }

    @Override
    public double evaluate(final FitnessFunction.NamedArgument... arguments) {
        final Map<String, FitnessFunction.NamedArgument> map = Arrays.stream(arguments)
            .collect(
                Collectors.toMap(
                    FitnessFunction.NamedArgument::getName,
                    Function.identity()
                )
            );
        double result = 0.0;
        for (final FitnessFunction.NamedArgument arg : arguments) {
            final CompositeFitnessFunction.Pair pair = this.pair(arg.getName());
            final FitnessFunction.NamedArgument[] args = pair.getFunction()
                .arguments()
                .stream()
                .map(map::get)
                .toArray(FitnessFunction.NamedArgument[]::new);
            if (args.length != pair.getFunction().arguments().size()) {
                throw new IllegalStateException(
                    String.format(
                        "Function %s expected more arguments",
                        pair.getFunction().getClass().getSimpleName()
                    )
                );
            }
            final double value = pair.getFunction().evaluate(args);
            result += pair.getWeight() * value;
        }
        return result;
    }

    /**
     * Finds a pair by the name of the argument that the function handles.
     * This assumes that only one function handles a particular argument name.
     * @param name The argument name
     * @return A {@link CompositeFitnessFunction.Pair} instance or throws a
     *  runtime exception
     */
    private CompositeFitnessFunction.Pair pair(final String name) {
        for (final CompositeFitnessFunction.Pair pair : this.pairs) {
            if (pair.getFunction().arguments().contains(name)) {
                return pair;
            }
        }
        throw new IllegalStateException(
            String.format(
                "No function has been registered to handle an argument with name %s",
                name
            )
        );
    }

    @Override
    public List<String> arguments() {
        return this.pairs.stream()
            .map(CompositeFitnessFunction.Pair::getFunction)
            .map(FitnessFunction::arguments)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    /**
     * A pair function-weight.
     */
    @Value
    private static class Pair {

        /**
         * The fitness function.
         */
        FitnessFunction function;

        /**
         * The percentage weight of the fitness score in the overall score.
         */
        double weight;

    }

}
