package com.rigiresearch.middleware.coordinator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rigiresearch.middleware.metamodels.SerializationParser;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
     * The properties configuration.
     */
    private final Configuration config;

    /**
     * A repository of Terraform templates.
     */
    private final TerraformRepository repository;

    /**
     * An API client for IBM CAM.
     */
    private final CamClient client;

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
        this.config = config;
        this.repository = new TerraformRepository(
            new URIish(config.getString("coordinator.repository.url")),
            config.getString("coordinator.repository.token")
        );
        this.client = new CamClient(this.config).setup();
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
            final JsonNode imports = node.get("imports");
            if (spec != null) {
                final Specification specification =
                    (Specification) this.serialization.asEObjects(
                        StringEscapeUtils.unescapeJava(spec.asText()),
                        URI.createFileURI("tmp.tf")
                    ).get(0);
                this.repository.update(specification);
            }
            if (vals != null) {
                this.updateStack(vals, imports);
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

    /**
     * Updates (or creates) a CAM stack for the current repository and given
     * values.
     * @param values The stack's parameter values
     * @param imports Resources to import into Terraform's state
     */
    private void updateStack(final JsonNode values, final JsonNode imports) {
        try {
            final Map<String, String> params = this.client.requestParameters();
            final String branch =
                this.repository.getBranch().getName().replace("refs/heads/", "");
            final JsonNode template = this.client.findOrCreateTemplate(
                this.config.getString("coordinator.repository.url"),
                branch,
                params
            );
            final JsonNode stack = this.client.findOrCreateStack(
                template.at("/id").textValue(),
                branch,
                params
            );
            final Map<String, String> imps = EvolutionCoordination.toJavaMap(imports);
            final Map<String, String> vals = EvolutionCoordination.toJavaMap(values);
            // FIXME Since this is the first Plan, the stack requires a value for all vars
            vals.put("allow_unverified_ssl", "true");
            final String stackid = stack.at("/id").textValue();
            this.client.performPlan(stackid, vals, params);
            final JsonNode plan =
                this.client.waitForActionOnStack("PLAN", stackid, params);
            for (final Map.Entry<String, String> entry : imps.entrySet()) {
                EvolutionCoordination.LOGGER.info(
                    "TODO terraform import {} {}",
                    entry.getKey(),
                    entry.getValue()
                );
            }
            if ("SUCCESS_WITH_CHANGES".equals(plan.at("/status").textValue())) {
                this.client.performApply(stackid, params);
                this.client.waitForActionOnStack("APPLY", stackid, params);
                // TODO in case the APPLY fails, create a Github issue
            }
        } catch (final IOException | URISyntaxException exception) {
            EvolutionCoordination.LOGGER.error("Error updating the stack", exception);
        }
    }

    /**
     * Translates a JSON node back to map.
     * @param node The input node
     * @return A non-null map
     */
    private static Map<String, String> toJavaMap(final JsonNode node) {
        final Map<String, String> values = new HashMap<>(node.size());
        final Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            final String field = fields.next();
            values.put(field, node.get(field).textValue());
        }
        return values;
    }

}
