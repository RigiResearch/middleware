/*
 * Copyright 2020 University of Victoria.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.rigiresearch.middleware.experimentation.software.kscript.model;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1ServicePortBuilder;
import io.kubernetes.client.util.Yaml;
import lombok.Value;

/**
 * A host-container port mapping.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Value
public final class KServicePortMapping implements Exportable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2519273051632375057L;

    /**
     * The port name.
     */
    private final String name;

    /**
     * The protocol name.
     */
    private final String protocol;

    /**
     * The target port.
     */
    private final int target;

    /**
     * The service port.
     */
    private final int port;

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KServicePortMapping(final V1ServicePort model) {
        this(
            model.getName(),
            model.getProtocol(),
            model.getTargetPort().getIntValue(),
            model.getPort()
        );
    }

    /**
     * Default constructor.
     * @param name The port name
     * @param protocol The protocol
     * @param target The target port
     * @param port The port
     */
    public KServicePortMapping(final String name, final String protocol,
        final int target, final int port) {
        this.name = name;
        this.protocol = protocol;
        this.target = target;
        this.port = port;
    }

    @Override
    public String toString() {
        return this.yaml();
    }

    @Override
    public String yaml() {
        return Yaml.dump(this.model());
    }

    /**
     * The Kubernetes client's model.
     * @return A non-null model object
     */
    public V1ServicePort model() {
        return new V1ServicePortBuilder()
            .withName(this.name)
            .withProtocol(this.protocol)
            .withTargetPort(new IntOrString(this.target))
            .withPort(this.port)
            .build();
    }

    /**
     * Deep copies this mapping into a new one.
     * @return A non-null object
     */
    public KServicePortMapping copy() {
        return new KServicePortMapping(
            this.name,
            this.protocol,
            this.target,
            this.port
        );
    }

}
