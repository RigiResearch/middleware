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
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
     * @throws MalformedURLException If any of the URLs is malformed
     */
    private List<Monitor> monitors(final Scheduler scheduler)
        throws MalformedURLException {
        final String[] paths = this.config.getStringArray("paths");
        final List<Monitor> monitors = new ArrayList<>(paths.length);
        for (final String path : paths) {
            final String url = this.config.getString(
                String.format("%s.url", path)
            );
            final String expression = this.config.getString(
                String.format("%s.expression", path)
            );
            final String[] params = this.config.getStringArray(
                String.format("%s.parameters", path)
            );
            final List<Parameter> parameters = new ArrayList<>(params.length);
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
