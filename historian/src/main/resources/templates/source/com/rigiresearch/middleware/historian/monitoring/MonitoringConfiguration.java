package com.rigiresearch.middleware.historian.monitoring;

import it.sauronsoftware.cron4j.Scheduler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration of the monitors and their execution.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class MonitoringConfiguration {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(MonitoringConfiguration.class);

    /**
     * The properties configuration.
     */
    private final Configuration config = MonitoringConfiguration.initialize();

    /**
     * A cron-like scheduler.
     */
    private final Scheduler scheduler = new Scheduler();

    /**
     * The Javers instance to use.
     */
    private final Javers javers;

    /**
     * An executor service.
     */
    private final ExecutorService executor;

    /**
     * The configured monitors.
     */
    private final List<Monitor> monitors;

    /**
     * Default constructor.
     */
    public MonitoringConfiguration() {
        this.javers = JaversBuilder.javers().build();
        this.executor = Executors.newFixedThreadPool(
            this.config.getInt("thread.pool.size", 30)
        );
        this.monitors = this.setupMonitors();
    }

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
     * Starts the monitors.
     * @throws InterruptedException See {@link ExecutorService#invokeAll(Collection)}
     */
    public void startMonitoring() throws InterruptedException {
        MonitoringConfiguration.LOGGER.info("Starting the monitors...");
        this.executor.invokeAll(this.monitors);
        this.scheduler.start();
    }

    /**
     * Stops the monitors.
     */
    public void stopMonitoring() {
        MonitoringConfiguration.LOGGER.info("Shutting down the monitors...");
        this.monitors.forEach(Monitor::stop);
        this.scheduler.stop();
        this.executor.shutdown();
    }

    /**
     * Instantiates and configures the list of monitors based on the properties
     * file "monitoring.properties" (See {@link #initialize()}).
     * @return A list of monitors
     */
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private List<Monitor> setupMonitors() {
        final String login = this.config.getString("auth");
        return Arrays.stream(
            this.config.getStringArray("monitors")
        )
            .map(path -> {
                final Monitor monitor = new Monitor(
                    path,
                    path,
                    this.config,
                    this.scheduler,
                    this.javers,
                    this.collector(path),
                    this.dependent(path),
                    this.executor
                );
                // TODO move login to a separate method
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
    private List<Monitor.DependentMonitor> dependent(final String path) {
        final List<String> ids =
            Arrays.asList(this.config.getStringArray("monitors"));
        final String key = String.format("%s.children", path);
        return Arrays.stream(this.config.getStringArray(key))
            .map(child -> {
                final String expression = this.config.getString(
                    String.format("%s.expression", child)
                );
                if (ids.contains(child)) {
                    throw new IllegalArgumentException(
                        String.format(
                            "Dependent monitor '%s' must not be listed as a monitor",
                            child
                        )
                    );
                }
                if (expression != null) {
                    MonitoringConfiguration.LOGGER.warn(
                        String.format(
                            "Cron expression for dependent monitor '%s' will be ignored",
                            child
                        )
                    );
                }
                final String input =
                    String.format("%s.children.%s.input", path, child);
                final String selector =
                    String.format("%s.inputs.%s.selector", child, input);
                return new Monitor.DependentMonitor(
                    this.config.getString(selector),
                    new Monitor(
                        child,
                        child,
                        this.config,
                        this.scheduler,
                        this.javers,
                        this.collector(child),
                        this.dependent(child),
                        this.executor
                    )
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Instantiates a data collector.
     * @param path The path to which this collector is associated
     * @return A data collector
     */
    private Collector collector(final String path) {
        final URL url;
        try {
            url = new URL(this.config.getString(String.format("%s.url", path)));
        } catch (final MalformedURLException exception) {
            MonitoringConfiguration.LOGGER.error("Malformed path URL", exception);
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
            if (username.isEmpty()) {
                MonitoringConfiguration.LOGGER.warn(
                    "Authentication for path '{}' contains an empty username",
                    path
                );
            }
            if (password.isEmpty()) {
                MonitoringConfiguration.LOGGER.warn(
                    "Authentication for path '{}' contains an empty password",
                    path
                );
            }
            provider = new BasicCredentialsProvider();
            provider.setCredentials(
                new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(username, password)
            );
        }
        return new Collector(
            path,
            new Request(url, inputs, provider),
            this.outputs(path)
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

}
