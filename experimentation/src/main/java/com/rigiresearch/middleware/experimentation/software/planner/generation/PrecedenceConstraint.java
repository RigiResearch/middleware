package com.rigiresearch.middleware.experimentation.software.planner.generation;

import com.rigiresearch.middleware.experimentation.software.planner.IndexSearch;
import java.util.List;
import lombok.Value;

/**
 * A precedence constraint.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
@Value
public class PrecedenceConstraint<T> implements Constraint<T> {

    /**
     * The element of interest.
     */
    T element;

    /**
     * Illegal preceding elements.
     */
    T precedingElement;

    /**
     * The minimum expected distance between the preceding element and the
     * element of interest.
     */
    int expectedDistance;

    /**
     * Constructs a constraint in which the element of interest must not be
     * preceded at all by the preceding element.
     * @param element The element of interest
     * @param precedingElement The illegal preceding element
     */
    public PrecedenceConstraint(final T element, final T precedingElement) {
        this(element, precedingElement, Integer.MAX_VALUE);
    }

    /**
     * Constructs a constraint in which the element of interest must be preceded
     * at least by certain distance.
     * @param element The element of interest
     * @param precedingElement The preceding element
     * @param expectedDistance The minimum expected distance
     */
    public PrecedenceConstraint(final T element, final T precedingElement,
        final int expectedDistance) {
        this.element = element;
        this.precedingElement = precedingElement;
        this.expectedDistance = expectedDistance;
    }

    @Override
    public boolean holds(final Chromosome<T> chromosome) {
        final List<T> all = chromosome.allComponents();
        final List<Integer> elements =
            new IndexSearch<T>(this.element, all).indices();
        final List<Integer> preceding =
            new IndexSearch<T>(this.precedingElement, all).indices();
        final boolean response;
        if (!elements.isEmpty() && !preceding.isEmpty()) {
            response = elements.stream()
                .allMatch(index -> this.holds0(index, preceding));
        } else {
            response = true;
        }
        return response;
    }

    /**
     * Determines whether an instance of the element of interest is in compliance
     * with all the instances of the preceding element.
     * @param index The index of the particular instance of interest
     * @param precedingIndices The indices of the preceding elements
     * @return {@code true} if this constraint holds for the element instance,
     *  {@code false} otherwise
     */
    private boolean holds0(final int index, final List<Integer> precedingIndices) {
        return precedingIndices.stream()
            .allMatch(precedingIndex -> {
                final boolean response;
                if (precedingIndex < index) {
                    response = index - precedingIndex >= this.expectedDistance;
                } else {
                    // It's not a preceding element, so the constraint does not apply
                    response = true;
                }
                return response;
            });
    }

}
