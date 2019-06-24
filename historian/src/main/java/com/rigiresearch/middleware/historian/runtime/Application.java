package com.rigiresearch.middleware.historian.runtime;

import it.sauronsoftware.cron4j.Scheduler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class Application {

    /**
     * The properties configuration.
     */
    private final Configuration config = initialize();

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
     * files "monitoring.properties" (See {@link #initialize()}).
     * @param scheduler The task scheduler
     * @return A list of monitors
     * @throws MalformedURLException If any of the URLs is malformed
     */
    public List<Monitor> monitors(final Scheduler scheduler)
        throws MalformedURLException {
        final List<Monitor> monitors = new ArrayList<>();
        for (final String path : this.config.getStringArray("paths")) {
            final List<Parameter> parameters = new ArrayList<>();
            final String url = this.config.getString(
                String.format("%s.url", path)
            );
            final String expression = this.config.getString(
                String.format("%s.expression", path)
            );
            final String[] params = this.config.getStringArray(
                String.format("%s.parameters", path)
            );
            for (final String param : params) {
                final String value = this.config.getString(
                    String.format("%s.%s.value", path, param)
                );
                final Parameter.Location location = Parameter.Location.valueOf(
                    this.config.getString(
                        String.format("%s.%s.location", path, param)
                    )
                );
                parameters.add(new Parameter(param, value, location));
            }
            monitors.add(
                new Monitor(new URL(url), parameters, expression, scheduler)
            );
        }
        return monitors;
    }

    /**
     * The main entry point.
     * @param args The program arguments
     * @throws MalformedURLException If any of the URL is malformed
     * @throws InterruptedException If something goes wrong starting the monitors
     */
    public static void main(final String... args)
        throws MalformedURLException, InterruptedException {
        final Application application = new Application();
        final Scheduler scheduler = new Scheduler();
        final List<Monitor> monitors = application.monitors(scheduler);
        final ExecutorService exec = Executors.newFixedThreadPool(monitors.size());
        exec.invokeAll(monitors);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                monitors.forEach(m -> m.stop());
                scheduler.stop();
                exec.shutdown();
            }
        });
    }
}
