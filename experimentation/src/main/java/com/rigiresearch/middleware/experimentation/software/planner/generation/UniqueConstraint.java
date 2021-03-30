package com.rigiresearch.middleware.experimentation.software.planner.generation;

import java.util.List;
import lombok.Value;

/**
 * The specified element must be unique within a chromosome.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
@Value
public class UniqueConstraint<T> implements Constraint<T> {

    T unique;

    @Override
    public boolean holds(final Chromosome<T> chromosome) {
        final List<T> list = chromosome.allComponents();
        return list.indexOf(this.unique) == list.lastIndexOf(this.unique);
    }

}
