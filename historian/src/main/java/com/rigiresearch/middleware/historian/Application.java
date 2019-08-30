package com.rigiresearch.middleware.historian;

import com.beust.jcommander.ParameterException;
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
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class.
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
     * The bindings resource name.
     */
    private static final String BINDINGS = "bindings.xml";

    /**
     * The arguments configuration.
     */
    private final CliArguments arguments;

    /**
     * The main entry point.
     * @param args The application arguments
     */
    @SuppressWarnings("PMD.DoNotCallSystemExit")
    public static void main(final String... args) {
        final CliArguments config = new CliArguments();
        try {
            config.parse(args);
            if (config.getParsedCommand().isPresent()) {
                final Application application = new Application(config);
                application.run(config.getParsedCommand().get());
            } else {
                config.usage();
            }
        } catch (final ParameterException exception) {
            Application.LOGGER.error(exception.getMessage());
            System.exit(1);
        } catch (final IOException | JAXBException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
            System.exit(1);
        }
    }

    /**
     * Run this application.
     * @param command The action to perform
     * @throws JAXBException If there is an error parsing the graph
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    private void run(final CliArguments.Command command)
        throws IOException, JAXBException {
        switch (command) {
            case GENERATE:
                if (this.arguments.getGenerate().isHelp()) {
                    this.arguments.usage(command);
                } else {
                    this.generate(this.arguments.getGenerate());
                }
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported action \"%s\"", command)
                );
        }
    }

    /**
     * Run this application.
     * @param generate The generate command
     * @throws JAXBException If there is an error parsing the graph
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private void generate(final CliArguments.GenerateCommand generate)
        throws IOException, JAXBException {
        switch (generate.getType()) {
            case DOT:
                this.generateDot(generate.getInput(), generate.getOutput());
                break;
            case PROJECT:
                this.generateProject(
                    this.model(generate.getInput()),
                    generate.getOutput()
                );
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported option \"%s\"", generate.getType())
                );
        }
    }

    /**
     * Transforms the OpenAPI specification into a monitoring model.
     * @param specification An OpenAPI specification
     * @return The model instance
     * @throws UnsupportedEncodingException If the encoding is not supported
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private Root model(final File specification)
        throws UnsupportedEncodingException, IOException {
        final edu.uoc.som.openapi.Root root = new OpenAPIImporter()
            .createOpenAPIModelFromJson(specification);
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
     * @param directory The output directory
     * @throws JAXBException If there is an exception during the graph
     *  marshalling
     * @throws IOException If there is an error with the jar file or saving the
     *  resource
     */
    private void generateProject(final Root model, final File directory)
        throws JAXBException, IOException {
        final File parent = new File(directory, "src/main/resources/");
        if (!parent.mkdirs() && !parent.exists()) {
            throw new IOException(
                String.format(
                    "Could not create directory %s",
                    parent.getAbsolutePath()
                )
            );
        }
        final GraphParser parser = new GraphParser()
            .withBindings(Application.BINDINGS);
        final Graph<Node> graph = this.monitoringGraph(model);
        parser.write(graph, new File(parent, "configuration.xml"));
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

    /**
     * Generates a DOT specification based on the given graph.
     * @param graph The configuration graph
     * @param directory The output directory
     * @throws JAXBException If there is an exception during the graph
     *  marshalling
     */
    private void generateDot(final File graph, final File directory)
        throws JAXBException {
        new GraphTemplate().generateFile(
            new GraphParser()
                .withBindings(Application.BINDINGS)
                .instance(graph),
            directory
        );
    }

}
