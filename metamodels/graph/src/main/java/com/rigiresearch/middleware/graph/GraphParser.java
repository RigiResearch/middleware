package com.rigiresearch.middleware.graph;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
     * The classes to include in the Jaxb context.
     */
    private static final Class<?>[] CLASS = {Graph.class};

    /**
     * Default buffer size.
     */
    private static final int BUFFER_SIZE = 8 * 1024;

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
     * @param <T> The subtype of {@link Node}
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> Graph<T> instance(final File file)
        throws JAXBException {
        return (Graph<T>) JAXBContext.newInstance(
            GraphParser.CLASS,
            this.properties
        ).createUnmarshaller()
            .unmarshal(file);
    }

    /**
     * Unmarshalls a graph instance.
     * @param input An input stream
     * @param <T> The subtype of {@link Node}
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     * @throws IOException If there's an I/O error
     */
    public <T extends Node> Graph<T> instance(final InputStream input)
        throws JAXBException, IOException {
        if (input != null) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final byte[] buffer = new byte[GraphParser.BUFFER_SIZE];
            int length = input.read(buffer);
            while (length > 0) {
                output.write(buffer, 0, length);
                length = input.read(buffer);
            }
            input.close();
            output.close();
            return this.instance(output.toString());
        }
        throw new IllegalArgumentException("Input stream is null");
    }

    /**
     * Unmarshalls a graph instance.
     * @param xml The XML content from which the graph is unmarshalled
     * @param <T> The subtype of {@link Node}
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> Graph<T> instance(final String xml)
        throws JAXBException {
        return (Graph<T>) JAXBContext.newInstance(
            GraphParser.CLASS,
            this.properties
        ).createUnmarshaller()
            .unmarshal(new StreamSource(new StringReader(xml)));
    }

    /**
     * Marshalls a graph instance into an XML file.
     * @param graph The graph instance
     * @param file The target file
     * @param <T> The subtype of {@link Node}
     * @throws JAXBException If there is an error marshalling the graph
     */
    public <T extends Node> void write(final Graph<T> graph, final File file)
        throws JAXBException {
        final Marshaller marshaller = JAXBContext.newInstance(
            GraphParser.CLASS,
            this.properties
        ).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(graph, file);
    }

}
