package com.rigiresearch.middleware.historian.runtime;

import com.rigiresearch.middleware.historian.runtime.json.JsonNodeXpathVisitor;
import com.vmware.xpath.json.JsonXpath;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A value extracted from a string based on an Xpath selector. This
 * implementation currently supports JSON content.
 * TODO Add support for XML content.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class XpathValue {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(XpathValue.class);

    /**
     * A constant error format.
     */
    private static final String ERROR_FORMAT = "Empty result for selector '%s'";

    /**
     * A JSON mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The content.
     */
    private final String content;

    /**
     * The Xpath selector.
     */
    private final String selector;

    /**
     * Finds a single node selected by the associated Xpath selector.
     * @return A non-null Json node selected from the input content
     * @throws IOException If something bad happens while selecting the node
     */
    public JsonNode singleNode() throws IOException {
        final JsonNode node = XpathValue.MAPPER.readTree(this.content);
        if (!JsonXpath.exists(node, this.selector)) {
            XpathValue.LOGGER.error(
                String.format(XpathValue.ERROR_FORMAT, this.selector)
            );
        }
        return JsonXpath.find(node, this.selector);
    }

    /**
     * Finds a single value selected by the associated Xpath selector.
     * @return A string value from the input content
     * @throws IOException If something bad happens while selecting the value
     */
    public String singleValue() throws IOException {
        return this.singleNode().asText();
    }

    /**
     * Finds the Json nodes selected by the associated Xpath selector.
     * @return A list of matching nodes
     * @throws IOException If something bad happens while selecting the nodes
     */
    public ArrayNode nodeArray() throws IOException {
        final JsonNode node = XpathValue.MAPPER.readTree(this.content);
        final JsonNodeXpathVisitor visitor = new JsonNodeXpathVisitor();
        JsonXpath.findAndUpdateMultiple(node, this.selector, visitor);
        if (visitor.getResult().size() == 0) {
            XpathValue.LOGGER.error(
                String.format(XpathValue.ERROR_FORMAT, this.selector)
            );
        }
        return visitor.getResult();
    }

    /**
     * Finds the values selected by the associated Xpath selector.
     * @return A list of matching values
     * @throws IOException If something bad happens while selecting the values
     */
    public Collection<String> values() throws IOException {
        return StreamSupport.stream(this.nodeArray().spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toSet());
    }

}
