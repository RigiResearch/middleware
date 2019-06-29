package com.rigiresearch.middleware.historian.runtime;

import com.vmware.xpath.json.JsonXpath;
import it.sauronsoftware.cron4j.Scheduler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
@RequiredArgsConstructor
public class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * HTTP 200 status code.
     */
    private static final int OK_CODE = 200;

    /**
     * The properties configuration.
     */
    private final Configuration config = Application.initialize();

    /**
     * Loads the configuration file.
     * @return A {@link Configuration} instance.
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static Configuration initialize() {
        final Parameters params = new Parameters();
        final FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class
            ).configure(
                params.properties()
                    .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                    .setFileName("monitoring.properties")
            );
        try {
            return builder.getConfiguration();
        } catch (final ConfigurationException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Instantiates and configures the list of monitors based on the properties
     * file "monitoring.properties" (See {@link #initialize()}).
     * @param scheduler The task scheduler
     * @return A list of monitors
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private List<Monitor> monitors(final Scheduler scheduler) {
        final String login = this.config.getString("login");
        return Arrays.stream(
            this.config.getStringArray("paths")
        )
        .map(path -> {
            final URL url;
            try {
                url = new URL(this.config.getString(String.format("%s.url", path)));
            } catch (final MalformedURLException exception) {
                Application.LOGGER.error("Malformed path URL", exception);
                throw new RuntimeException(exception);
            }
            final String expression =
                this.config.getString(String.format("%s.expression", path));
            final List<Parameter> parameters =
                Arrays.stream(
                    this.config.getStringArray(String.format("%s.parameters", path))
                )
                .map(name -> this.parameter(path, name))
                .collect(Collectors.toList());
            final String username =
                this.config.getString(String.format("%s.username", path));
            final String password =
                this.config.getString(String.format("%s.password", path));
            CredentialsProvider provider = null;
            if (username != null && password != null) {
                provider = new BasicCredentialsProvider();
                provider.setCredentials(
                    new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(username, password)
                );
            }
            final Monitor monitor = new Monitor(
                path,
                scheduler,
                new Request(url, parameters, provider),
                () -> expression
            );
            if (path.equals(login)) {
                monitor.callback(this.loginCallback());
                monitor.collect();
            }
            return monitor;
        })
        .collect(Collectors.toList());
    }

    /**
     * Instantiates a {@link Parameter}.
     * @param path The path to which this parameter is associated
     * @param name The parameter name
     * @return A {@link Parameter}
     */
    private Parameter parameter(final String path, final String name) {
        return new Parameter(
            name,
            () -> this.config.getString(String.format("%s.%s.value", path, name)),
            Parameter.Location.valueOf(
                this.config.getString(String.format("%s.%s.location", path, name))
            )
        );
    }

    /**
     * Sets up the login callback.
     * @return The callback
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private Monitor.Callback loginCallback() {
        return response -> {
            final int status = response.getStatusLine().getStatusCode();
            if (status != Application.OK_CODE) {
                throw new RuntimeException(
                    String.format("Unsuccessful login. Status code is ", status)
                );
            }
            final String[] parameters =
                this.config.getStringArray("login.output.parameters");
            final String type = response.getEntity()
                .getContentType()
                .getValue();
            switch (type) {
                case "application/json":
                    final ObjectMapper mapper = new ObjectMapper();
                    final JsonNode content = mapper.readTree(
                        response.getEntity().getContent()
                    );
                    Arrays.stream(parameters)
                        .forEach(param -> {
                            final String selector = this.config.getString(
                                String.format("login.%s.selector", param)
                            );
                            this.config.setProperty(
                                String.format("login.%s.value", param),
                                JsonXpath.find(content, selector).asText()
                            );
                        });
                    break;
                case "application/xml":
                    // TODO Add support for XML content
                default:
                    Application.LOGGER.error("Unsupported content type '{}'", type);
                    break;
            }
        };
    }

    /**
     * The main entry point.
     * @param args The program arguments
     * @throws InterruptedException If something goes wrong starting the monitors
     */
    public static void main(final String... args)
        throws InterruptedException {
        final Scheduler scheduler = new Scheduler();
        final List<Monitor> monitors = new Application().monitors(scheduler);
        final ExecutorService exec = Executors.newFixedThreadPool(monitors.size());
        Application.LOGGER.info("Starting the monitors...");
        exec.invokeAll(monitors);
        scheduler.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                Application.LOGGER.info("Shutting down the monitors...");
                monitors.forEach(m -> m.stop());
                scheduler.stop();
                exec.shutdown();
            }
        });
    }
}
