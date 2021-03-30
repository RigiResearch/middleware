package com.rigiresearch.middleware.experimentation.software.planner.generation;

/**
 * A structural constraint over chromosomes.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
public interface Constraint<T> {

    /**
     * Whether this constraint holds given a particular chromosome.
     * @param chromosome The chromosome
     * @return {@code true} if this constraint holds or {@code false}
     */
    boolean holds(Chromosome<T> chromosome);

}
