package com.rigiresearch.middleware.experimentation.reification;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.experimentation.reification.fitness.FitnessResult;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A solution space exploration method.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface SolutionSpaceExploration {

    /**
     * Explores the solution space, finding an average value for the given
     * metric based on associated variables.
     * @param metric The metric to explore
     * @param graph The metric dependency graph
     * @param fitness A function that evaluates the fitness of a combination of
     *  variable values
     */
    List<Map<String, Double>> explore(Metric metric, Graph<? extends Node> graph,
        Function<Map<String, Double>, FitnessResult> fitness);

}
