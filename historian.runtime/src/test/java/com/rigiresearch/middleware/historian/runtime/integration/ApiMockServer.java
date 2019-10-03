package com.rigiresearch.middleware.historian.runtime.integration;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * A container for mocking the vcenter API.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
public final class ApiMockServer extends GenericContainer {

    /**
     * The API definition file.
     */
    private static final String API_FILE = "/vcenter.json";

    /**
     * The host port to which the container port is mapped.
     */
    private static final int HOST_PORT = 80;

    /**
     * The container port.
     */
    private static final int CONTAINER_PORT = 4010;

    /**
     * The expected response code.
     */
    private static final int EXPECTED_CODE = 200;

    /**
     * Default constructor.
     */
    ApiMockServer() {
        super("stoplight/prism:2");
    }

    /**
     * Configures this container.
     * @return This container
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    ApiMockServer setup() {
        this.addFixedExposedPort(
            ApiMockServer.HOST_PORT,
            ApiMockServer.CONTAINER_PORT
        );
        this.withClasspathResourceMapping(
            "vcenter.json",
            ApiMockServer.API_FILE,
            BindMode.READ_ONLY
        );
        this.waitingFor(
            Wait.forHttp("/vcenter/vm")
                .forStatusCode(ApiMockServer.EXPECTED_CODE)
        );
        this.withCommand("mock", "-h", "0.0.0.0", ApiMockServer.API_FILE);
        return this;
    }

}
