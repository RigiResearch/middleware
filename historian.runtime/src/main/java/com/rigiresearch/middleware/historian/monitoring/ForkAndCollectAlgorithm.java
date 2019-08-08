package com.rigiresearch.middleware.historian.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Output;
import com.rigiresearch.middleware.graph.Parameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.commons.configuration2.Configuration;

/**
 * An implementation of the Fork and Collect algorithm.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class ForkAndCollectAlgorithm {

    /**
     * A JSON object mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * A graph instance configured with input/output dependencies.
     */
    private final Graph<Monitor> graph;

    /**
     * Monitors for which the collection processed.
     */
    private final Collection<Monitor> released;

    /**
     * Default constructor.
     * @param graph The dependency graph
     * @param config The configuration properties
     */
    public ForkAndCollectAlgorithm(final Graph<Node> graph,
        final Configuration config) {
        this.config = config;
        this.graph = new Graph<>(
            graph.getNodes()
                .stream()
                .map(node -> new Monitor(node, config))
                .collect(Collectors.toSet())
        );
        this.released = new ArrayList<>(this.graph.getNodes().size());
    }

    /**
     * Recursive method to realize the fork and collect strategy.
     * @return A Json object representing the collected content
     * @throws IOException If there is an issue either collecting the content or
     *  mapping it to Json objects
     * @throws UnexpectedResponseCodeException If a request resturns an
     *  unexpected response code
     */
    public JsonNode data() throws IOException, UnexpectedResponseCodeException {
        // TODO Validate the graph. Look for cycles. Move to Graph?
        this.released.clear();
        // Initial fork step
        final Collection<Monitor> branches = this.graph.getNodes()
            .stream()
            .filter(node -> node.dependencies().isEmpty())
            .collect(Collectors.toSet());
        return this.data(branches);
    }

    /**
     * Recursive method to realize the fork and collect strategy.
     * @param branches The current branches
     * @return A Json object representing the collected content
     * @throws IOException If there is an issue either collecting the content or
     *  mapping it to Json objects
     * @throws UnexpectedResponseCodeException If a request resturns an
     *  unexpected response code
     */
    private JsonNode data(final Collection<Monitor> branches)
        throws IOException, UnexpectedResponseCodeException {
        final JsonNode result = this.node(branches);
        for (final Monitor branch : branches) {
            // Collect step
            final String content = branch.collect();
            final JsonNode object = ForkAndCollectAlgorithm.MAPPER.readTree(content);
            this.add(result, branch.getIdentifier(), object);
            this.released.add(branch);
            final Collection<ResultSet<String, String>> located =
                this.collectedValues(branch, content);
            final Collection<ResultSet<String, String>> singletons = located.stream()
                .filter(ResultSet::isSingleton)
                .collect(Collectors.toList());
            final Collection<ResultSet<String, String>> multivalued = located.stream()
                .filter(set -> !set.isSingleton())
                .collect(Collectors.toList());
            for (final ResultSet<String, String> values : multivalued) {
                // Fork step
                final Collection<Monitor> next =
                    this.branches(branch, singletons, values);
                // Recursive call
                if (next.isEmpty()) {
                    continue;
                }
                final JsonNode data = this.data(next);
                final String name = next.iterator().next().getName();
                this.add(result, name, data);
            }
        }
        return result;
    }

    /**
     * Creates monitor instances (i.e., branches) based on the collected values
     * for a branch's outputs.
     * @param branch The branch
     * @param singletons Values corresponding to single-value outputs
     * @param values Values corresponding to multivalued outputs
     * @return A set of branches from which new content will be collected
     */
    private Collection<Monitor> branches(final Monitor branch,
        final Collection<ResultSet<String, String>> singletons,
        final ResultSet<String, String> values) {
        final Collection<Monitor> branches =
            new HashSet<>(this.graph.getNodes().size());
        this.graph.dependents(branch)
            .stream()
            .filter(node -> !this.released.contains(node))
            .filter(node -> this.released.containsAll(node.dependencies()))
            .forEach(node ->
                values.forEach(entry -> {
                    final String name = entry.getKey();
                    final String value = entry.getValue();
                    final Monitor tmp = new Monitor(node, this.config);
                    final String bid = branch.getIdentifier();
                    final String tid = tmp.getIdentifier();
                    tmp.setIdentifier("%s-%s-%s", bid, tid, value);
                    tmp.setValue(name, value);
                    singletons.forEach(set ->
                        set.forEach(e ->
                            tmp.setContextValue(e.getKey(), e.getValue())
                        )
                    );
                    // Transfer the context from the previous branch
                    branch.getContextValues().forEach(tmp::setContextValue);
                    branches.add(tmp);
                })
            );
        return branches;
    }

    /**
     * Locates outputs for a given branch in the collected content.
     * @param branch The branch
     * @param content The collected content
     * @return A set of result sets containing key-vaue pairs, where the key is
     *  the name of the output
     * @throws IOException In case there is a problem locating an Xpath selector
     */
    private Collection<ResultSet<String, String>> collectedValues(
        final Monitor branch, final String content) throws IOException {
        final Collection<ResultSet<String, String>> collections =
            new ArrayList<>(0);
        for (final Parameter parameter : branch.getParameters(true)) {
            if (parameter instanceof Output) {
                final Output output = (Output) parameter;
                final XpathValue value = new XpathValue(content, output.getSelector());
                if (output.isMultivalued()) {
                    final Collection<String> values = value.values();
                    final ResultSet<String, String> set =
                        new ResultSet<>(false);
                    values.forEach(str -> set.addEntry(output.getName(), str));
                    collections.add(set);
                } else {
                    collections.add(
                        new ResultSet<>(output.getName(), value.value())
                    );
                }
            }
        }
        return collections;
    }

    /**
     * Creates a Json node based on the current branches.
     * @param branches The current branches
     * @return An array if all the branches share the nanme, an object otherwise
     */
    private JsonNode node(final Collection<Monitor> branches) {
        final JsonNode object;
        String name = null;
        boolean equal = !branches.isEmpty();
        final Iterator<Monitor> iterator = branches.iterator();
        while (iterator.hasNext()) {
            final Monitor tmp = iterator.next();
            if (name == null) {
                name = tmp.getName();
            }
            if (!name.equals(tmp.getName())) {
                equal = false;
                break;
            }
        }
        if (equal) {
            object = ForkAndCollectAlgorithm.MAPPER.createArrayNode();
        } else {
            object = ForkAndCollectAlgorithm.MAPPER.createObjectNode();
        }
        return object;
    }

    /**
     * Adds the given Json object to a source Json node.
     * The latter can be either an object or an array.
     * @param source The Json node to which the object is added
     * @param identifier A unique name for the property name
     * @param object The Json node to add
     */
    private void add(final JsonNode source, final String identifier,
        final JsonNode object) {
        if (source instanceof ObjectNode) {
            ((ObjectNode) source).set(identifier, object);
        } else if (source instanceof ArrayNode && object instanceof ArrayNode) {
            final ArrayNode tmp = (ArrayNode) object;
            if (tmp.size() > 0) {
                ((ArrayNode) source).addAll(tmp);
            }
        } else if (source instanceof ArrayNode) {
            ((ArrayNode) source).add(object);
        }
    }

}
