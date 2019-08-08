package com.rigiresearch.middleware.graph;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.jaxb.JAXBContextProperties;

/**
 * A {@link Graph} parser.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class GraphParser {

    /**
     * A Jaxb properties map.
     */
    private final Map<String, Object> properties;

    /**
     * Default constructor.
     */
    public GraphParser() {
        this.properties = new HashMap<>(0);
    }

    /**
     * Configures the binding option.
     * @param filenames The bindings resource names
     * @return This parser (for chaining)
     */
    public GraphParser withBindings(final String... filenames) {
        final List<InputStream> streams = new ArrayList<>(filenames.length);
        Arrays.stream(filenames)
            .forEach(
                filename -> streams.add(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(filename)
                )
            );
        this.properties.put(JAXBContextProperties.OXM_METADATA_SOURCE, streams);
        return this;
    }

    /**
     * Unmarshalls a graph instance.
     * @param file The XML file from which the graph is unmarshalled
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     */
    @SuppressWarnings("unchecked")
    public Graph<Graph.Node> instance(final File file)
        throws JAXBException {
        final Class<?>[] classes = {Graph.class};
        return (Graph<Graph.Node>) JAXBContext.newInstance(classes, this.properties)
            .createUnmarshaller()
            .unmarshal(file);
    }

    /**
     * Unmarshalls a graph instance.
     * @param xml The XML content from which the graph is unmarshalled
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     */
    @SuppressWarnings("unchecked")
    public Graph<Graph.Node> instance(final String xml)
        throws JAXBException {
        final Class<?>[] classes = {Graph.class};
        return (Graph<Graph.Node>) JAXBContext.newInstance(classes, this.properties)
            .createUnmarshaller()
            .unmarshal(new StreamSource(new StringReader(xml)));
    }

    /**
     * Marshalls a graph instance into an XML file.
     * @param graph The graph instance
     * @param file The target file
     * @throws JAXBException If there is an error marshalling the graph
     */
    public void write(final Graph<? extends Graph.Node> graph, final File file)
        throws JAXBException {
        final Class<?>[] classes = {graph.getClass()};
        final Marshaller marshaller = JAXBContext.newInstance(classes, this.properties)
            .createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(graph, file);
    }

}
