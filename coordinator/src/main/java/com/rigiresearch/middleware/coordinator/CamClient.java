package com.rigiresearch.middleware.coordinator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for interacting with IBM CAM.
 * TODO Use the refresh token
 * FIXME See {@link #initClient()}
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
@SuppressWarnings({
    "checkstyle:ClassDataAbstractionCoupling",
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods"
})
public final class CamClient {

    /**
     * The tenant id.
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * The ICP team.
     */
    public static final String ICP_TEAM = "ace_orgGuid";

    /**
     * The ICP namespace.
     */
    public static final String ICP_NAMESPACE = "cloudOE_spaceGuid";

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(CamClient.class);

    /**
     * A JSON object mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * An insecure HTTP client.
     * FIXME Change HTTP client initialization.
     */
    private static final CloseableHttpClient CLIENT = CamClient.initClient();

    /**
     * Initial capcity for maps/collections.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * HTTP 200 status code.
     */
    private static final int OKAY = 200;

    /**
     * Charset UTF-8.
     */
    private static final String UTF_8 = "UTF-8";

    /**
     * JSON content type.
     */
    private static final String JSON = "application/json";

    /**
     * The property name for the access token.
     */
    private static final String ACCESS_TOKEN = "cam.auth.token";

    /**
     * The access token's expiration time.
     */
    private static final String ACCESS_TOKEN_EXP = "cam.auth.token.expiration";

    /**
     * The type of access token.
     */
    private static final String ACCESS_TOKEN_TYPE = "cam.auth.token.type";

    /**
     * The property name for CAM's URL.
     */
    private static final String CAM_URL = "cam.url";

    /**
     * The properties configuration.
     */
    private final Configuration config;

    /**
     * Initializes an HTTP client that does not check SSL certificates.
     * FIXME This is done to bypass security checks for CAM servers.
     *  This is very insecure!
     * Adapted from:
     *  - https://stackoverflow.com/a/19518383/738968
     *  - https://stackoverflow.com/a/19519566/738968
     * @return An HTTP client
     */
    private static CloseableHttpClient initClient() {
        final SSLContextBuilder builder = new SSLContextBuilder();
        try {
            // builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            builder.loadTrustMaterial(null, (chain, type) -> true);
            final LayeredConnectionSocketFactory factory =
                new SSLConnectionSocketFactory(
                    builder.build(),
                    NoopHostnameVerifier.INSTANCE
                );
            return HttpClients.custom().setSSLSocketFactory(factory).build();
        } catch (final NoSuchAlgorithmException | KeyStoreException
            | KeyManagementException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Configures this client.
     * @return This client
     */
    public CamClient setup() {
        this.authenticate();
        // Perform the request 10s before the token expires
        final long secs = 10L;
        final long delay = this.config.getLong(CamClient.ACCESS_TOKEN_EXP) - secs;
        Executors.newScheduledThreadPool(1)
            .scheduleAtFixedRate(this::authenticate, delay, delay, TimeUnit.SECONDS);
        CamClient.LOGGER.debug("Scheduled authentication for CAM. Exp. time is {}s", delay);
        return this;
    }

    /**
     * Requests an authentication token.
     */
    public void authenticate() {
        final HttpPost request = new HttpPost(this.config.getString("cam.auth.url"));
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("charset", CamClient.UTF_8);
        try {
            request.setEntity(
                new StringEntity(
                    String.format(
                        "grant_type=password&username=%s&password=%s&scope=openid",
                        this.config.getString("cam.user"),
                        this.config.getString("cam.password")
                    )
                )
            );
            final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
            final int code = response.getStatusLine().getStatusCode();
            if (code == CamClient.OKAY) {
                CamClient.LOGGER.info(
                    "Collected authentication token (CAM)"
                );
                final JsonNode node =
                    CamClient.MAPPER.readTree(response.getEntity().getContent());
                this.config.setProperty(
                    CamClient.ACCESS_TOKEN,
                    node.at("/access_token").textValue()
                );
                this.config.setProperty(
                    CamClient.ACCESS_TOKEN_EXP,
                    node.at("/expires_in").longValue()
                );
                this.config.setProperty(
                    CamClient.ACCESS_TOKEN_TYPE,
                    node.at("/token_type").textValue()
                );
                CamClient.LOGGER.debug(
                    "Collected {} token {} (CAM)",
                    this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                    this.config.getString(CamClient.ACCESS_TOKEN)
                );
            } else {
                CamClient.LOGGER.error("Unexpected response code {} (#authenticate)", code);
            }
        } catch (final IOException exception) {
            CamClient.LOGGER.error("Authentication error (CAM)", exception);
        }
    }

    /**
     * Requests CAM the tenant id, ICP team and ICP namespace.
     * @return A map with the collected values
     * @throws IOException If there's an I/O error
     */
    public Map<String, String> requestParameters() throws IOException {
        final Map<String, String> map = new HashMap<>(CamClient.INITIAL_CAPACITY);
        final HttpGet request = new HttpGet(
            String.format(
                "%s/cam/tenant/api/v1/tenants/getTenantOnPrem",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code == CamClient.OKAY) {
            map.put(CamClient.TENANT_ID, body.at("/id").textValue());
            JsonNode namespace = null;
            // TODO Don't use the default namespace but let the user specify it
            for (final JsonNode tmp : body.at("/namespaces")) {
                if ("default".equals(tmp.at("/uid").textValue())) {
                    namespace = tmp;
                    break;
                }
            }
            map.put(CamClient.ICP_TEAM, namespace.at("/teamId").textValue());
            map.put(CamClient.ICP_NAMESPACE, namespace.at("/uid").textValue());
        } else {
            CamClient.LOGGER.error("Unexpected response code {} (#requestParameters)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        CamClient.LOGGER.debug("Collected CAM parameters {}", map);
        return map;
    }

    /**
     * Creates a Terraform template using HCL as format.
     * @param repository The (remote) repository URL
     * @param token The personal access token
     * @param branch The repository branch containing the CAM template and the
     *  Terraform files
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    @SuppressWarnings({
        "checkstyle:ExecutableStatementCount",
        "checkstyle:ParameterNumber"
    })
    public JsonNode createTemplate(final String repository, final String token,
        final String branch, final Map<String, String> params)
        throws URISyntaxException, IOException {
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/templates/createFromSource",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final ObjectNode input = CamClient.MAPPER.createObjectNode();
        final ObjectNode source = CamClient.MAPPER.createObjectNode();
        final ObjectNode github = CamClient.MAPPER.createObjectNode();
        input.set("template_type", CamClient.MAPPER.valueToTree("Terraform"));
        input.set("template_format", CamClient.MAPPER.valueToTree("HCL"));
        github.set("url", CamClient.MAPPER.valueToTree(repository));
        github.set("token", CamClient.MAPPER.valueToTree(token));
        github.set("dir", CamClient.MAPPER.valueToTree(""));
        github.set("ref", CamClient.MAPPER.valueToTree(branch));
        source.set("github", github);
        input.set("template_source", source);
        request.setEntity(
            new StringEntity(
                CamClient.MAPPER.writeValueAsString(input),
                CamClient.UTF_8
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error("Unexpected response code {} (#createTemplate)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Finds templates matching with a given repository.
     * @param repository The (remote) repository URL
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body for the matching templates or empty if no
     * template matches the given repository URL
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public ArrayNode templatesForRepository(final String repository,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final ArrayNode elements = CamClient.MAPPER.createArrayNode();
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/templates",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpGet request = new HttpGet(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code == CamClient.OKAY) {
            for (final JsonNode template : body) {
                // TODO Validate that template_provider is vSphere?
                final JsonNode url =
                    template.at("/manifest/template_source/github/url");
                if (!url.isMissingNode() && url.textValue().equals(repository)) {
                    elements.add(template);
                }
            }
        } else {
            CamClient.LOGGER.error(
                "Unexpected response code {} (#templatesForRepository)",
                code
            );
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return elements;
    }

    /**
     * Finds a template version matching with the given template id and branch.
     * @param template The template id
     * @param branch The repository's branch
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body for the matching template version
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode templateVersion(final String template, final String branch,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/templateVersions",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpGet request = new HttpGet(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .addParameter(
                    "filter",
                    String.format(
                        "{\"where\": {\"templateId\": \"%s\", \"ref\": \"%s\"}}",
                        template,
                        branch
                    )
                )
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        JsonNode version = CamClient.MAPPER.createObjectNode();
        if (code == CamClient.OKAY) {
            if (body.size() > 0) {
                // It should only return one (name/ref must be unique)
                version = body.get(0);
            }
        } else {
            CamClient.LOGGER.error("Unexpected response code {} (#templateVersion)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return version;
    }

    /**
     * Finds templates matching with a given repository and branch. If at least
     * one template matches the repository but not the branch, it creates a
     * template version. If none match, it creates a template.
     * @param repository The (remote) repository URL
     * @param branch The repository's branch
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The corresponding template
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode findOrCreateTemplate(final String repository,
        final String branch, final Map<String, String> params)
            throws URISyntaxException, IOException {
        final ArrayNode templates = this.templatesForRepository(repository, params);
        final JsonNode template;
        if (templates.size() > 0) {
            // TODO In case there are several matching templates, which one
            //  should be selected? They're conceptually the same!
            template = templates.get(0);
            final String identifier = template.at("/id").textValue();
            final JsonNode version = this.templateVersion(identifier, branch, params);
            if (version == null || version.size() == 0) {
                // There's at least one matching template but the branch is different
                this.createTemplateVersion(identifier, branch, params);
            }
        } else {
            template = this.createTemplate(
                this.config.getString("coordinator.repository.url"),
                this.config.getString("coordinator.repository.token"),
                branch,
                params
            );
            CamClient.LOGGER.debug("Created template {}", template.at("/name").textValue());
        }
        return template;
    }

    /**
     * Finds stacks matching with a given template and branch. If at least
     * one template matches the repository but not the branch, it creates a
     * template version. If none match, it creates a template.
     * @param template The template id
     * @param branch The repository's branch
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The corresponding template
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode findOrCreateStack(final String template, final String branch,
        final Map<String, String> params) throws URISyntaxException, IOException {
        // final JsonNode version = this.templateVersion(template, branch, params);
        final ArrayNode stacks = this.stacksForTemplate(template, params);
        final JsonNode stack;
        if (stacks.size() > 0) {
            // TODO Which stack should I use? I'm taking the first one always
            stack = this.retrieveStack(
                stacks.get(0).at("/id").textValue(),
                params
            );
        } else {
            stack = this.createStack(
                String.format("%s-%s-imported", branch, template),
                "Imported resources from VMware vSphere",
                template,
                this.config.getString("cam.vsphere.connection"),
                params
            );
        }
        return stack;
    }

    /**
     * Creates a Stack.
     * @param name The stack's name
     * @param description A description
     * @param template The template id
     * @param connection The connection id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws IOException If there's an I/O error
     * @throws URISyntaxException If there's an error building the request URI
     */
    @SuppressWarnings({
        "checkstyle:ParameterNumber",
        "PMD.UseObjectForClearerAPI"
    })
    public JsonNode createStack(final String name, final String description,
        final String template, final String connection,
            final Map<String, String> params) throws IOException, URISyntaxException {
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/stacks",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final ObjectNode input = CamClient.MAPPER.createObjectNode();
        final ArrayNode connections = CamClient.MAPPER.createArrayNode();
        input.set("name", CamClient.MAPPER.valueToTree(name));
        input.set("description", CamClient.MAPPER.valueToTree(description));
        input.set("templateId", CamClient.MAPPER.valueToTree(template));
        connections.add(connection);
        input.set("cloud_connection_ids", connections);
        request.setEntity(
            new StringEntity(
                CamClient.MAPPER.writeValueAsString(input),
                CamClient.UTF_8
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error("Unexpected response code {} (#createStack)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Performs a Plan action on a particular stack.
     * @param stack The stack's id
     * @param values The stack's parameter values
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws IOException If there's an I/O error
     * @throws URISyntaxException If there's an error building the request URI
     */
    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public JsonNode performPlan(final String stack,
        final Map<String, String> values, final Map<String, String> params)
        throws IOException, URISyntaxException {
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/stacks/%s/plan",
                this.config.getString(CamClient.CAM_URL),
                stack
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final ObjectNode input = CamClient.MAPPER.createObjectNode();
        final ArrayNode value = CamClient.MAPPER.createArrayNode();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            final ObjectNode item = CamClient.MAPPER.createObjectNode();
            item.set("name", CamClient.MAPPER.valueToTree(entry.getKey()));
            item.set("value", CamClient.MAPPER.valueToTree(entry.getValue()));
            value.add(item);
        }
        input.set("parameters", value);
        request.setEntity(
            new StringEntity(
                CamClient.MAPPER.writeValueAsString(input),
                CamClient.UTF_8
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code == CamClient.OKAY) {
            CamClient.LOGGER.info("Started a Terraform plan");
        } else {
            CamClient.LOGGER.error("Unexpected response code {} (#performPlan)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Waits until an action finishes.
     * Delay for retry is 5s.
     * @param action The type of action
     * @param stack The stack id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The stack's data when the action's status changed from IN_PROGRESS
     * @throws IOException If there's an I/O error
     * @throws URISyntaxException If there's an error building the request URI
     */
    public JsonNode waitForActionOnStack(final String action, final String stack,
        final Map<String, String> params) throws IOException, URISyntaxException {
        JsonNode retrieved;
        final long delay = 5000L;
        do {
            CamClient.LOGGER.debug("Waiting for {} to finish", action);
            try {
                Thread.sleep(delay);
            } catch (final InterruptedException exception) { }
            retrieved = this.retrieveStack(stack, params);
        } while (action.equals(retrieved.at("/action").textValue())
            && "IN_PROGRESS".equals(retrieved.at("/status").textValue()));
        CamClient.LOGGER.info(
            "{} action ended with status {}",
            action,
            retrieved.at("/status").textValue()
        );
        return retrieved;
    }

    /**
     * Performs an Apply action on a particular stack.
     * @param stack The stack's id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws IOException If there's an I/O error
     * @throws URISyntaxException If there's an error building the request URI
     */
    public JsonNode performApply(final String stack,
        final Map<String, String> params) throws IOException, URISyntaxException {
        final URI base  = new URI(
            String.format(
                "%s/cam/api/v1/stacks/%s/apply",
                this.config.getString(CamClient.CAM_URL),
                stack
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code == CamClient.OKAY) {
            CamClient.LOGGER.info("Started a Terraform apply");
        } else {
            CamClient.LOGGER.error("Unexpected response code {} (#performApply)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Retrieves information about a Stack.
     * @param stack The stack's id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode retrieveStack(final String stack,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final URI base = new URI(
            String.format(
                "%s/cam/api/v1/stacks/%s/retrieve",
                this.config.getString(CamClient.CAM_URL),
                stack
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error("Unexpected response code {} (#retrieveStack)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Retrieves information about a Template.
     * @param template The template's id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode retrieveTemplate(final String template,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final URI base = new URI(
            String.format(
                "%s/cam/api/v1/templates/%s",
                this.config.getString(CamClient.CAM_URL),
                template
            )
        );
        final HttpGet request = new HttpGet(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error("Unexpected response code {} (#retrieveTemplate)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Retrieves a list of stacks for a particular template.
     * @param template The template's id
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public ArrayNode stacksForTemplate(final String template,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final URI base = new URI(
            String.format(
                "%s/cam/api/v1/stacks",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpGet request = new HttpGet(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .addParameter(
                    "filter",
                    String.format(
                        "{\"where\": {\"templateId\": \"%s\"}}",
                        template
                    )
                )
                .build()
        );
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final ArrayNode body =
            (ArrayNode) CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error("Unexpected response code {} (#stacksForTemplate)", code);
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

    /**
     * Creates a template version.
     * @param template The template's id
     * @param branch The new version (A.K.A. the source repository's branch)
     * @param params Request parameters (tenant id, ICP team & namespace)
     * @return The response's body
     * @throws URISyntaxException If there's an error building the request URI
     * @throws IOException If there's an I/O error
     */
    public JsonNode createTemplateVersion(final String template, final String branch,
        final Map<String, String> params) throws URISyntaxException, IOException {
        final URI base = new URI(
            String.format(
                "%s/cam/api/v1/templateVersions",
                this.config.getString(CamClient.CAM_URL)
            )
        );
        final HttpPost request = new HttpPost(
            new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort())
                .setPath(base.getPath())
                .addParameter(CamClient.TENANT_ID, params.get(CamClient.TENANT_ID))
                .addParameter(CamClient.ICP_TEAM, params.get(CamClient.ICP_TEAM))
                .addParameter(CamClient.ICP_NAMESPACE, params.get(CamClient.ICP_NAMESPACE))
                .build()
        );
        request.setHeader("Content-Type", CamClient.JSON);
        request.setHeader("Accept", CamClient.JSON);
        request.setHeader("charset", CamClient.UTF_8);
        request.setHeader(
            "Authorization",
            String.format(
                "%s %s",
                this.config.getString(CamClient.ACCESS_TOKEN_TYPE),
                this.config.getString(CamClient.ACCESS_TOKEN)
            )
        );
        final ObjectNode input = CamClient.MAPPER.createObjectNode();
        input.set("name", CamClient.MAPPER.valueToTree(branch));
        input.set("ref", CamClient.MAPPER.valueToTree(branch));
        input.set("enabled", CamClient.MAPPER.valueToTree("true"));
        input.set("templateId", CamClient.MAPPER.valueToTree(template));
        request.setEntity(
            new StringEntity(
                CamClient.MAPPER.writeValueAsString(input),
                CamClient.UTF_8
            )
        );
        final CloseableHttpResponse response = CamClient.CLIENT.execute(request);
        final int code = response.getStatusLine().getStatusCode();
        final JsonNode body =
            CamClient.MAPPER.readTree(response.getEntity().getContent());
        if (code != CamClient.OKAY) {
            CamClient.LOGGER.error(
                "Unexpected response code {} (#createTemplateVersion)",
                code
            );
            CamClient.LOGGER.error(CamClient.MAPPER.writeValueAsString(body));
        }
        return body;
    }

}
