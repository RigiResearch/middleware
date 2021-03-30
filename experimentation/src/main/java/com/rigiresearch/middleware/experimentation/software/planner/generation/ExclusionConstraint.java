package com.rigiresearch.middleware.experimentation.software.planner.generation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;

/**
 * A group of elements are exclusive within a chromosome.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.3.2
 */
public class ExclusionConstraint<T> implements Constraint<T> {

    @Getter
    List<T> exclusive;

    public ExclusionConstraint(final T... exclusive) {
        this.exclusive = new ObjectArrayList<>(exclusive);
    }

    @Override
    public boolean holds(final Chromosome<T> chromosome) {
        final List<T> all = chromosome.allComponents();
        boolean response = true;
        boolean exists = false;
        for (final T tmp : this.exclusive) {
            final boolean contains = all.contains(tmp);
            if (contains && exists) {
                response = false;
                break;
            } else if (contains) {
                exists = true;
            }
        }
        return response;
    }

}
