package com.rigiresearch.middleware.experimentation.reification;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import java.util.Map;
import java.util.function.Function;

/**
 * A knowledge reification method that explores combinations of inputs to
 * approximate a function that describes a particular metric.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface KnowledgeReification {

    /**
     * Explores the solution space and approximates a function for the given metric.
     * @param metric The metric to reify
     * @param graph The metric dependency graph
     * @param fitness A function that evaluates the fitness of a combination of
     *  variable values
     */
    DifferentiableFunction reify(Metric metric, Graph<? extends Node> graph,
        Function<Map<String, Object>, Double> fitness);

}
