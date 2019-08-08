package com.rigiresearch.middleware.historian;

import com.rigiresearch.middleware.graph.Graph;
import com.rigiresearch.middleware.graph.GraphParser;
import com.rigiresearch.middleware.graph.Input;
import com.rigiresearch.middleware.graph.Node;
import com.rigiresearch.middleware.graph.Parameter;
import com.rigiresearch.middleware.historian.templates.GraphTemplate;
import com.rigiresearch.middleware.historian.templates.MonitoringTemplate;
import com.rigiresearch.middleware.metamodels.AtlTransformation;
import com.rigiresearch.middleware.metamodels.monitoring.LocatedProperty;
import com.rigiresearch.middleware.metamodels.monitoring.MonitoringPackage;
import com.rigiresearch.middleware.metamodels.monitoring.Root;
import edu.uoc.som.openapi.OpenAPIPackage;
import edu.uoc.som.openapi.io.OpenAPIImporter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class.
 * TODO use jcommander to handle the parameters.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public final class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Application.class);

    /**
     * The path to an OpenAPI spec.
     */
    private final String spec;

    /**
     * The path to a directory where the monitoring code will be generated.
     */
    @Getter
    private final String target;

    /**
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Expected a JSON OpenAPI specification and target directory as input"
            );
        }
        try {
            final Application application = new Application(args[0], args[1]);
            application.generateFiles(application.model());
        } catch (final IOException | JAXBException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
        }
    }

    /**
     * Transforms the OpenAPI specification into a monitoring model.
     * @return The model instance
     * @throws UnsupportedEncodingException If the encoding is not supported
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private Root model() throws UnsupportedEncodingException, IOException {
        final edu.uoc.som.openapi.Root root = new OpenAPIImporter()
            .createOpenAPIModelFromJson(new File(this.spec));
        final String output = "OUT";
        return (Root) new AtlTransformation.Builder()
            .withMetamodel(MonitoringPackage.eINSTANCE)
            .withMetamodel(OpenAPIPackage.eINSTANCE)
            .withModel(AtlTransformation.ModelType.INPUT, "IN", root)
            .withModel(AtlTransformation.ModelType.OUTPUT, output, "monitoring.xmi")
            .withTransformation("src/main/resources/atl/OpenAPI2Monitoring.atl")
            .build()
            .run()
            .get(output)
            .getResource()
            .getContents()
            .get(0);
    }

    /**
     * Generates a gradle project containing the monitoring code for a
     * particular API specification.
     * @param model The monitoring model
     * @throws JAXBException If there is an exception during the graph
     *  marshalling
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private void generateFiles(final Root model)
        throws JAXBException, IOException {
        final File directory = new File(this.target);
        final GraphParser parser = new GraphParser()
            .withBindings("bindings.xml");
        final Graph<Node> graph = this.monitoringGraph(model);
        parser.write(graph, new File(directory, "configuration.xml"));
        new MonitoringTemplate().generateFiles(model, directory);
        new GraphTemplate().generateFile(graph, directory);
    }

    /**
     * Transforms a monitoring model into a graph.
     * @param model The monitoring model
     * @return The graph instance
     */
    private Graph<Node> monitoringGraph(final Root model) {
        return new Graph<Node>(
            model.getMonitors()
                .stream()
                .map(monitor -> {
                    final Set<Parameter> parameters = monitor.getPath()
                        .getParameters()
                        .stream()
                        // TODO add option to generate all inputs
                        .filter(LocatedProperty::isRequired)
                        .map(property -> new Input(property.getName(), ""))
                        .collect(Collectors.toSet());
                    return new Node(
                        monitor.getPath().getId(),
                        parameters,
                        Collections.emptySet()
                    );
                })
                .collect(Collectors.toSet())
        );
    }

}
