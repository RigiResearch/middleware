package com.rigiresearch.middleware.vmware.hcl.agent;

import com.rigiresearch.middleware.historian.runtime.UnexpectedResponseCodeException;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Application {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Application.class);

    /**
     * Default constructor.
     */
    private Application() {
    }

    /**
     * The main entry point.
     * @param args The program arguments
     */
    public static void main(final String... args) {
        try {
            final RuntimeAgent agent = new RuntimeAgent();
            agent.start();
            Runtime.getRuntime()
                .addShutdownHook(new Thread(agent::stop));
        } catch (final IOException | ConfigurationException | JAXBException
            | UnexpectedResponseCodeException exception) {
            Application.LOGGER.error(exception.getMessage(), exception);
        }
    }

}
