package com.rigiresearch.middleware.experimentation.reification.impl;

import com.rigiresearch.middleware.experimentation.graph.Metric;
import com.rigiresearch.middleware.experimentation.reification.DifferentiableFunction;
import com.rigiresearch.middleware.experimentation.reification.KnowledgeReification;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link KnowledgeReification} that uses grid search to
 * explore combinations of values.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class GridSearch implements KnowledgeReification {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GridSearch.class);

    @Override
    public DifferentiableFunction reify(final Metric metric,
        final Graph<? extends Node> graph,
        final Function<Map<String, Object>, Double> fitness) {
        throw new UnsupportedOperationException("#reify()");
    }

}
