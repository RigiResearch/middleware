package com.rigiresearch.middleware.historian.runtime.json;

import com.vmware.xpath.json.JsonXpathVisitor;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

/**
 * A simple visitor that keeps the visited JSON nodes.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class JsonNodeXpathVisitor implements JsonXpathVisitor {

    /**
     * The collection of selected nodes.
     */
    @Getter
    private final ArrayNode result;

    /**
     * Default constructor.
     */
    public JsonNodeXpathVisitor() {
        this.result = new ObjectMapper().createArrayNode();
    }

    @Override
    public boolean visit(final JsonNode parent, final JsonNode current) {
        this.result.add(current);
        return true;
    }

    /**
     * Set of distinct values.
     * @return A non-null set
     */
    public Set<String> getDistinctValues() {
        return StreamSupport.stream(this.result.spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toSet());
    }

}
