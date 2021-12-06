package com.rigiresearch.middleware.experimentation.reification;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import java.util.List;
import java.util.Map;

/**
 * A knowledge reification method that approximates a function that describes a
 * particular metric.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface KnowledgeReification {

    /**
     * Approximates a function for the given metric.
     * @param metric The metric to explore
     * @param graph The metric dependency graph
     * @param values A list of replications with variables and metrics
     */
    DifferentiableFunction reify(Metric metric, Graph<? extends Node> graph,
        List<Map<String, Double>> values);

    /**
     * TODO complete java doc.
     */
    List<Map<String, Double>> reify(SolutionSpaceExploration exploration);

}
