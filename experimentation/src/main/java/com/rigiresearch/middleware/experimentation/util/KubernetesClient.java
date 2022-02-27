package com.rigiresearch.middleware.experimentation.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Value;
import org.zeroturnaround.exec.StartedProcess;

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

    /**
     * Path of the kube config file.
     */
    private final String kubeconfig;

    /**
     * Default constructor.
     * @param directory The current directory
     * @param kubeconfig Path of the kube config file
     * @throws IOException If there is a problem initializing the client
     */
    public KubernetesClient(final File directory, final String kubeconfig)
        throws IOException {
        super(KubernetesClient.EXECUTABLE, directory);
        this.kubeconfig = kubeconfig;
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
        final List<String> args = new ArrayList<>();
        args.add("--kubeconfig");
        args.add(this.kubeconfig);
        return this.run("version", args, timeout, unit) == 0;
    }

    /**
     * Runs the "wait" command with condition "ready" for all pods.
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean waitUntilReady(final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        final List<String> args = new ArrayList<>();
        args.add("-n");
        args.add("default");
        args.add("--for=condition=ready");
        args.add("pod");
        args.add("--all");
        args.add(String.format("--timeout=%ds", unit.toSeconds(timeout)));
        args.add("--kubeconfig");
        args.add(this.kubeconfig);
        return this.run("wait", args, timeout, unit) == 0;
    }

    /**
     * Runs the "port-forward" command.
     * @param config The port forwarding config
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public StartedProcess portForward(final PortForwardConfig config,
        final long timeout, final TimeUnit unit)
        throws InterruptedException, IOException, TimeoutException {
        final List<String> args = new ArrayList<>();
        args.add("-n");
        args.add("default");
        args.add(String.format("svc/%s", config.getService()));
        args.add(String.format("%d:%d", config.getLocalport(), config.getPort()));
        args.add("--kubeconfig");
        args.add(this.kubeconfig);
        return this.start("port-forward", args, timeout, unit);
    }

    /**
     * Runs the "port-forward" command.
     * @param manifest The manifest file to deploy
     * @param timeout The timeout
     * @param unit The unit of time for the timeout
     * @return The command's exit value
     * @throws InterruptedException If the command is interrupted
     * @throws TimeoutException If the command takes longer than expected to finish
     * @throws IOException If there is an I/O error
     */
    public boolean apply(final String manifest, final long timeout,
        final TimeUnit unit) throws InterruptedException, IOException, TimeoutException {
        final List<String> args = new ArrayList<>();
        args.add("-n");
        args.add("default");
        args.add("-f");
        args.add(manifest);
        args.add("--kubeconfig");
        args.add(this.kubeconfig);
        return this.run("apply", args, timeout, unit) == 0;
    }

    /**
     * Configuration for port forwarding.
     */
    @Value
    public static class PortForwardConfig {

        /**
         * The service name.
         */
        String service;

        /**
         * The cluster port number.
         */
        int port;

        /**
         * The local port number.
         */
        int localport;

        /**
         * Timeout for pod deployment.
         */
        long timeout;

        /**
         * Timeout unit.
         */
        TimeUnit unit;

    }

}
