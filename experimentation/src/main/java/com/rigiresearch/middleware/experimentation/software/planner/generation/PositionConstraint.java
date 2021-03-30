package com.rigiresearch.middleware.experimentation.software.planner.generation;

import java.util.List;
import lombok.Value;

/**
 * The specified element must be found in the specified position, if it exists.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
@Value
public class PositionConstraint<T> implements Constraint<T> {

    /**
     * The constrained element.
     */
    T element;

    /**
     * The expected index if the element exists.
     */
    int index;

    @Override
    public boolean holds(final Chromosome<T> chromosome) {
        final List<T> all = chromosome.allComponents();
        final int position = all.indexOf(this.element);
        final boolean response;
        if (position == -1) {
            response = true;
        } else {
            // There's only one element (otherwise the constraint does not hold)
            // and its position is equal to the given index
            response = position == all.lastIndexOf(this.element) && position == this.index;
        }
        return response;
    }

}
