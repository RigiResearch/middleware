package com.rigiresearch.middleware.historian.runtime;

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
        final String login = this.config.getString("auth");
        return Arrays.stream(
            this.config.getStringArray("monitors")
        )
        .map(path -> {
            final String key = String.format("%s.expression", path);
            final String expression = this.config.getString(key);
            final Monitor monitor = new Monitor(
                path,
                scheduler,
                () -> expression,
                this.collector(path)
            );
            if (path.equals(login)) {
                // API authentication
                monitor.collector().collect();
            }
            return monitor;
        })
        .collect(Collectors.toList());
    }

    /**
     * Instantiates a {@link Input}.
     * @param path The path to which this parameter is associated
     * @param name The parameter name
     * @return A {@link Input}
     */
    private Input parameter(final String path, final String name) {
        final String value = String.format("%s.inputs.%s.value", path, name);
        final String location = String.format("%s.inputs.%s.location", path, name);
        return new Input(
            name,
            () -> this.config.getString(value),
            Input.Location.valueOf(
                this.config.getString(location)
            )
        );
    }

    /**
     * Instantiates a list of children data collector.
     * @param path The path to which these collectors are associated
     * @return A list of data collectors
     */
    private List<DataCollector.ChildDataCollector> children(final String path) {
        final List<String> monitors =
            Arrays.asList(this.config.getStringArray("monitors"));
        final String key = String.format("%s.children", path);
        return Arrays.stream(this.config.getStringArray(key))
            .map(child -> {
                final String expression = this.config.getString(
                    String.format("%s.expression", child)
                );
                if (monitors.contains(child)) {
                    throw new IllegalArgumentException(
                        String.format(
                            "Child monitor '%s' must not be listed as a monitor",
                            child
                        )
                    );
                }
                if (expression != null) {
                    Application.LOGGER.warn(
                        String.format(
                            "Cron expression for child monitor '%s' will be ignored",
                            child
                        )
                    );
                }
                final String input =
                    String.format("%s.children.%s.input", path, child);
                final String selector =
                    String.format("%s.inputs.%s.selector", child, input);
                return new DataCollector.ChildDataCollector(
                    this.config.getString(selector),
                    this.collector(child)
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Instantiates a data collector.
     * @param path The path to which this collector is associated
     * @return A data collector
     */
    private DataCollector collector(final String path) {
        final URL url;
        try {
            url = new URL(this.config.getString(String.format("%s.url", path)));
        } catch (final MalformedURLException exception) {
            Application.LOGGER.error("Malformed path URL", exception);
            throw new RuntimeException(exception);
        }
        final String key = String.format("%s.inputs", path);
        final List<Input> inputs =
            Arrays.stream(this.config.getStringArray(key))
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
        return new DataCollector(
            this.config,
            path,
            new Request(url, inputs, provider),
            this.outputs(path),
            this.children(path)
        );
    }

    /**
     * Instantiates a list of outputs associated with the given path.
     * @param path The path is
     * @return A list of outputs
     */
    private List<Output> outputs(final String path) {
        final String key = String.format("%s.outputs", path);
        return Arrays.stream(this.config.getStringArray(key))
            .map(name -> new Output(this.config, path, name))
            .collect(Collectors.toList());
    }

    /**
     * The main entry point.
     * TODO Move everything to a class MonitoringConfiguration
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
