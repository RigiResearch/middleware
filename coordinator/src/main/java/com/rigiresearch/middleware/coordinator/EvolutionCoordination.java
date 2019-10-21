package com.rigiresearch.middleware.coordinator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rigiresearch.middleware.metamodels.SerializationParser;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component for coordinating the specification evolution.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class EvolutionCoordination {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(EvolutionCoordination.class);

    /**
     * A JSON mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A repository of Terraform templates.
     */
    private final TerraformRepository repository;

    /**
     * An Ecore serialization parser.
     */
    private final SerializationParser serialization;

    /**
     * Default constructor.
     * @param config The configuration properties
     * @throws IOException If there's an I/O error
     * @throws GitAPIException If there's an error cloning the repository
     * @throws URISyntaxException If the repository's URL is malformed
     */
    public EvolutionCoordination(final Configuration config)
        throws IOException, GitAPIException, URISyntaxException {
        this.repository = new TerraformRepository(
            new URIish(config.getString("coordinator.repository.url")),
            config.getString("coordinator.repository.token")
        );
        this.serialization = new SerializationParser();
    }

    /**
     * Handles a specification update from run-time changes.
     * @param body The request body containing the specification and/or the values
     * @throws GitAPIException If there's a Git error updating the templates
     */
    public void runtimeUpdate(final String body) throws GitAPIException {
        try {
            final JsonNode node = EvolutionCoordination.MAPPER.readTree(body);
            final JsonNode spec = node.get("specification");
            final JsonNode vals = node.get("values");
            if (spec != null) {
                final Specification specification =
                    (Specification) this.serialization.asEObjects(
                        StringEscapeUtils.unescapeJava(spec.asText()),
                        URI.createFileURI("tmp.tf")
                    ).get(0);
                this.repository.update(specification);
            }
            if (vals != null) {
                EvolutionCoordination.LOGGER.info(
                    "New values received: {}",
                    EvolutionCoordination.MAPPER.writeValueAsString(vals)
                );
            }
        } catch (final IOException exception) {
            EvolutionCoordination.LOGGER.error(
                "I/O error updating the Terraform templates",
                exception
            );
            // TODO create issue with the error and the update
        } catch (final HclParsingException exception) {
            EvolutionCoordination.LOGGER.error(
                "Error parsing the Terraform templates from the repository",
                exception
            );
            // TODO create issue with the error and the update
        }
    }

}
