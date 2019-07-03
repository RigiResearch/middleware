package com.rigiresearch.middleware.historian.monitoring;

import com.vmware.xpath.json.DistinctTextValueJsonXpathVisitor;
import com.vmware.xpath.json.JsonXpath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A value extracted from a string based on a Xpath selector. This
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
     * The Xpath selector.
     */
    private final String selector;

    /**
     * The content.
     */
    private final String content;

    /**
     * A JSON mapper.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Finds a single value selected by the given Xpath selector.
     * @return a string value from the input content
     * @throws IOException If something bad happens while finding the value
     */
    public String value() throws IOException {
        JsonNode node = this.mapper.readTree(content);
        if (!JsonXpath.exists(node, this.selector)) {
            XpathValue.LOGGER.error(
                String.format(XpathValue.ERROR_FORMAT, this.selector)
            );
        }
        return JsonXpath.find(node, this.selector)
            .asText();
    }

    /**
     * Finds the values selected by the given Xpath selector.
     * @return A list of matching values
     * @throws IOException If something bad happens while finding the values
     */
    public List<String> values() throws IOException {
        final JsonNode node = this.mapper.readTree(content);
        final DistinctTextValueJsonXpathVisitor visitor =
            new DistinctTextValueJsonXpathVisitor(this.selector);
        JsonXpath.findAndUpdateMultiple(node, this.selector, visitor);
        final List<String> elements = new ArrayList<>(visitor.getDistinctSet());
        if (elements.isEmpty()) {
            XpathValue.LOGGER.error(
                String.format(XpathValue.ERROR_FORMAT, this.selector)
            );
        }
        return elements;
    }

}
