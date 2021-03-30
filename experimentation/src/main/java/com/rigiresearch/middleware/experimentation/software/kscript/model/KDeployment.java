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

import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1DeploymentBuilder;
import io.kubernetes.client.util.Yaml;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * A deployment resource.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@Accessors(fluent = true)
@Value
public final class KDeployment implements KResource {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 9188457182641525267L;

    /**
     * The deployment name.
     */
    private final String name;

    /**
     * The number of replicas.
     */
    private final int replicas;

    /**
     * The pod to deploy
     */
    private final KPod pod;

    /**
     * Instantiates this resource from the given model.
     * @param model The Kubernetes model
     */
    public KDeployment(final V1Deployment model) {
        this(
            model.getMetadata().getName(),
            model.getSpec().getReplicas(),
            new KPod(model.getSpec().getTemplate())
        );
    }

    /**
     * Default constructor.
     * @param name The deployment name
     * @param replicas The number of replicas
     * @param pod The pod to deploy
     */
    public KDeployment(final String name, final int replicas, final KPod pod) {
        this.name = name;
        this.replicas = replicas;
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
    public V1Deployment model() {
        return new V1DeploymentBuilder()
            .withApiVersion("apps/v1")
            .withKind("Deployment")
            .withNewMetadata()
            .withName(this.name)
            .endMetadata()
            .withNewSpec()
            .withReplicas(this.replicas)
            .withNewSelector()
            .withMatchLabels(this.pod.labels())
            .endSelector()
            // The pod template
            .withNewTemplate()
            .withNewMetadata()
            .withLabels(this.pod.labels())
            .endMetadata()
            .withNewSpec()
            .withContainers(
                this.pod.containers()
                    .stream()
                    .map(KContainer::model)
                    .collect(Collectors.toList())
            )
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();
    }

    @Override
    public KDeployment copy() {
        return new KDeployment(
            this.name,
            this.replicas,
            this.pod.copy()
        );
    }

    @Override
    public Map<String, String> labels() {
        return new HashMap<>(0);
    }

}
