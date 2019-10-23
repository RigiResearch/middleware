package com.rigiresearch.middleware.coordinator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
            if (spec != null) {
                final Specification specification =
                    (Specification) this.serialization.asEObjects(
                        StringEscapeUtils.unescapeJava(spec.asText()),
                        URI.createFileURI("tmp.tf")
                    ).get(0);
                this.repository.update(specification);
            }
            if (vals != null) {
                this.updateStack(vals);
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
     * TODO improve this method
     * @param values The stack's parameter values
     */
    @SuppressWarnings({
        "checkstyle:CyclomaticComplexity",
        "checkstyle:ExecutableStatementCount",
        "checkstyle:NPathComplexity",
        "PMD.NPathComplexity",
        "PMD.CyclomaticComplexity"
    })
    private void updateStack(final JsonNode values) {
        try {
            final Map<String, String> params = this.client.requestParameters();
            final ArrayNode templates = this.client.templatesForRepository(
                this.config.getString("coordinator.repository.url"),
                params
            );
            final String branch =
                this.repository.getBranch().getName().replace("refs/heads/", "");
            JsonNode template = null;
            if (templates.size() > 0) {
                for (final JsonNode tmp : templates) {
                    final JsonNode ref = tmp.at("/manifest/template_source/github/ref");
                    if (!ref.isMissingNode() && ref.textValue().equals(branch)) {
                        template = tmp;
                        break;
                    }
                }
            }
            if (template == null) {
                template = this.client.createTemplate(
                    this.config.getString("coordinator.repository.url"),
                    this.config.getString("coordinator.repository.token"),
                    branch,
                    params
                );
                EvolutionCoordination.LOGGER.debug(
                    "Created template {}",
                    template.at("/name").textValue()
                );
            } else {
                EvolutionCoordination.LOGGER.debug(
                    "Using template {}",
                    template.at("/name").textValue()
                );
            }
            final ArrayNode stacks =
                this.client.stacksForTemplate(template.get("id").textValue(), params);
            final JsonNode stack;
            if (stacks.size() > 0) {
                // TODO Which stack should I use? I'm taking the first one always
                stack = this.client.retrieveStack(
                    stacks.get(0).at("/id").textValue(),
                    params
                );
            } else {
                stack = this.client.createStack(
                    String.format("%s-imported", template.at("/name").textValue()),
                    "Imported resources from VMware vSphere",
                    template.at("/id").textValue(),
                    this.config.getString("cam.vsphere.connection"),
                    params
                );
            }
            final Map<String, String> vals = new HashMap<>(values.size() + 1);
            // FIXME Since this is the first Plan, the stack requires a value for all vars
            vals.put("allow_unverified_ssl", "true");
            final Iterator<String> fields = values.fieldNames();
            while (fields.hasNext()) {
                final String field = fields.next();
                vals.put(field, values.get(field).textValue());
            }
            final String stackid = stack.at("/id").textValue();
            JsonNode plan =
                this.client.performPlan(stackid, vals, params);
            EvolutionCoordination.LOGGER.info("Performed a Terraform plan");
            if ("PLAN".equals(plan.at("/action").textValue())
                && "IN_PROGRESS".equals(plan.at("/status").textValue())) {
                final long delay = 3000L;
                do {
                    EvolutionCoordination.LOGGER.debug("Waiting for Plan to finish");
                    Thread.sleep(delay);
                    plan = this.client.retrieveStack(stackid, params);
                } while ("PLAN".equals(plan.at("/action").textValue())
                    && "IN_PROGRESS".equals(plan.at("/status").textValue()));
            }
            // TODO import VMs
            EvolutionCoordination.LOGGER.info("TODO Perform a Terraform import");
            if ("SUCCESS_WITH_CHANGES".equals(plan.at("/status").textValue())) {
                // TODO Apply
                EvolutionCoordination.LOGGER.info("TODO Perform a Terraform apply");
                // this.client.performApply(stackid, params);
            }
        } catch (final IOException | URISyntaxException | InterruptedException exception) {
            EvolutionCoordination.LOGGER.error("Error updating the stack", exception);
        }
    }

}
