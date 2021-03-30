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

import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceBuilder;
import io.kubernetes.client.util.Yaml;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * A Kubernetes service.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class KService implements KResource {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4314796089248916211L;

    /**
     * This service's name.
     */
    private final String name;

    /**
     * Selector labels.
     */
    private final Map<String, String> selector;

    /**
     * The ports mapping.
     */
    private final List<KServicePortMapping> ports;

    /**
     * The pod associated with this service.
     */
    private KPod pod;

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KService(final V1Service model) {
        this(
            model.getMetadata().getName(),
            model.getSpec().getSelector(),
            model.getSpec().getPorts().stream()
                .map(KServicePortMapping::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * Default constructor.
     * @param name The service name
     * @param selector The label selector
     * @param ports The ports
     */
    public KService(final String name, final Map<String, String> selector,
        final List<KServicePortMapping> ports) {
        this.name = name;
        this.selector = selector;
        this.ports = ports;
    }

    /**
     * Updates the pod associated with this service.
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

    @Override
    public V1Service model() {
        return new V1ServiceBuilder()
            .withApiVersion("v1")
            .withKind("Service")
            .withNewMetadata()
            .withName(this.name)
            .endMetadata()
            .withNewSpec()
            .withSelector(this.selector)
            .withPorts(
                this.ports.stream()
                    .map(KServicePortMapping::model)
                    .collect(Collectors.toList())
            )
            .endSpec()
            .build();
    }

    @Override
    public KService copy() {
        return new KService(
            this.name,
            new HashMap<>(this.selector),
            this.ports.stream()
                .map(KServicePortMapping::copy)
                .collect(Collectors.toList())
        );
    }

    @Override
    public Map<String, String> labels() {
        return new HashMap<>(0);
    }

}
