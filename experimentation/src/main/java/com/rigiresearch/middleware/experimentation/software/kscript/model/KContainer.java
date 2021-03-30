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

import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerBuilder;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1ContainerPortBuilder;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * A Pod Container.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class KContainer implements Exportable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 8966889861704832707L;

    /**
     * The container name.
     */
    private final String name;

    /**
     * The container image.
     */
    private final String image;

    /**
     * The exposed ports.
     */
    private final List<Integer> ports;

    /**
     * The pod to which this container belongs.
     */
    private KPod pod;

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KContainer(final V1Container model) {
        this(
            model.getName(),
            model.getImage(),
            model.getPorts().stream()
                .map(V1ContainerPort::getContainerPort)
                .collect(Collectors.toList())
        );
    }

    /**
     * Default constructor.
     * @param name The container name
     * @param image The container image
     * @param ports The exposed ports
     */
    public KContainer(final String name, final String image,
        final List<Integer> ports) {
        this.name = name;
        this.image = image;
        this.ports = ports;
    }

    /**
     * Updates the pod to which this container belongs.
     * @param pod The pod
     */
    public void pod(final KPod pod) {
        this.pod = pod;
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
    public V1Container model() {
        return new V1ContainerBuilder()
            .withName(this.name)
            .withImage(this.image)
            .withPorts(
                this.ports.stream()
                    .map(port -> new V1ContainerPortBuilder()
                        .withContainerPort(port)
                        .build())
                    .collect(Collectors.toList())
            )
            .build();
    }

    /**
     * Deep copies this container into a new one.
     * @return A non-null object
     */
    public KContainer copy() {
        return new KContainer(
            this.name,
            this.image,
            new ArrayList<>(this.ports)
        );
    }
}
