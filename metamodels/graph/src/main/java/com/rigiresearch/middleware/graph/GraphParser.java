package com.rigiresearch.middleware.graph;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.NoArgsConstructor;

/**
 * A {@link Graph} parser.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@NoArgsConstructor
public final class GraphParser {

    /**
     * Unmarshalls a graph instance.
     * @param file The XML file from which the graph is unmarshalled
     * @return The unmarshalled graph
     * @throws JAXBException If there is an error unmarshalling the graph
     */
    public Graph instance(final File file) throws JAXBException {
        return (Graph) JAXBContext.newInstance(Graph.class)
            .createUnmarshaller()
            .unmarshal(file);
    }

    /**
     * Marshalls a graph instance into an XML file.
     * @param graph The graph instance
     * @param file The target file
     * @throws JAXBException If there is an error marshalling the graph
     */
    public void write(final Graph graph, final File file) throws JAXBException {
        final Marshaller marshaller = JAXBContext.newInstance(Graph.class)
            .createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(graph, file);
    }

}
