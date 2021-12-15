package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A simple Kubernetes command line client.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class KubernetesClient extends AbstractCommandLineClient {

    /**
     * Kubernetes's executable name.
     */
    private static final String EXECUTABLE = "kubectl";

    public KubernetesClient(final File directory) throws IOException {
        super(KubernetesClient.EXECUTABLE, directory);
    }

    /**
     * Runs the "version" command.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean version(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        return this.run("--version", timeout, unit) == 0;
    }

}
