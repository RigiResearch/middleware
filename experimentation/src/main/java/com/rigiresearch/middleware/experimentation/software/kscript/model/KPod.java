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

import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodBuilder;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
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
 * A Kubernetes pod.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class KPod implements KResource {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -206854752196060279L;

    /**
     * This service's name.
     */
    private final String name;

    /**
     * Labels associated with this pod.
     */
    private final Map<String, String> labels;

    /**
     * The containers included in this pod.
     */
    private final List<KContainer> containers;

    /**
     * The service associated with this pod.
     */
    private KService service;

    /**
     * The deployment associated with this pod.
     */
    private KDeployment deployment;

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KPod(final V1Pod model) {
        this(
            model.getMetadata().getName(),
            model.getMetadata().getLabels(),
            model.getSpec().getContainers()
                .stream()
                .map(KContainer::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * Default constructor.
     * @param name The pod's name
     * @param labels The pod's labels
     * @param containers The containers to deploy
     */
    public KPod(final String name, final Map<String, String> labels,
        final List<KContainer> containers) {
        this.name = name;
        this.labels = labels;
        this.containers = containers;
    }

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KPod(final V1PodTemplateSpec model) {
        this(
            model.getMetadata().getName(),
            model.getMetadata().getLabels(),
            model.getSpec().getContainers()
                .stream()
                .map(KContainer::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * Adds a container to this pod.
     * @param container The container to add
     */
    public void add(final KContainer container) {
        container.pod(this);
        this.containers.add(container);
    }

    /**
     * Updates the service associated with this pod.
     * @param service The service
     */
    public void service(final KService service) {
        this.service = service;
    }

    /**
     * Updates the deployment associated with this pod.
     * @param deployment The deployment
     */
    public void deployment(final KDeployment deployment) {
        this.deployment = deployment;
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
    @Override
    public V1Pod model() {
        return new V1PodBuilder()
            .withApiVersion("v1")
            .withKind("Pod")
            .withNewMetadata()
            .withName(this.name)
            .withLabels(this.labels)
            .endMetadata()
            .withNewSpec()
            .withContainers(
                this.containers.stream()
                    .map(KContainer::model)
                    .collect(Collectors.toList())
            )
            .endSpec()
            .build();
    }

    @Override
    public KPod copy() {
        return new KPod(
            this.name,
            new HashMap<>(this.labels),
            new ArrayList<>(
                this.containers.stream()
                    .map(KContainer::copy)
                    .collect(Collectors.toList())
            )
        );
    }

}
