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

import io.kubernetes.client.util.Yaml;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A specification.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class KSpecification implements Exportable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4777109485300582574L;

    /**
     * A resource factory.
     */
    private static final KResourceFactory FACTORY = new KResourceFactory();

    /**
     * Initial list capacity.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * Kubernetes resources.
     */
    private final List<KResource> resources;

    /**
     * Configuration files to generate relative to the YAML source file.
     */
    private final Map<File, String> files;

    /**
     * Empty constructor.
     */
    public KSpecification() {
        this.resources = new ArrayList<>(KSpecification.INITIAL_CAPACITY);
        this.files = new HashMap<>(KSpecification.INITIAL_CAPACITY);
    }

    /**
     * Default constructor.
     * @param objects A collection of resources that belong to this specification
     */
    public KSpecification(final Collection<Object> objects) {
        this.resources = objects.stream()
            .map(KSpecification.FACTORY::resource)
            .collect(Collectors.toList());
        this.resources.forEach(this::updateRelationships);
        this.files = new HashMap<>(KSpecification.INITIAL_CAPACITY);
    }

    /**
     * Updates relationships for a resource.
     * @param resource The resource
     * @return The same resource
     */
    private KResource updateRelationships(final KResource resource) {
        if (resource instanceof KDeployment) {
            final KDeployment deployment = (KDeployment) resource;
            // Update the backwards relationship container->pod
            deployment.pod()
                .containers()
                .forEach(container -> container.pod(deployment.pod()));
            deployment.pod().deployment(deployment);
            this.updateServiceRelationships(deployment.pod());
        } else if (resource instanceof KService) {
            this.resources.stream()
                .filter(KPod.class::isInstance)
                .map(KPod.class::cast)
                .forEach(this::updateServiceRelationships);
            this.resources.stream()
                .filter(KDeployment.class::isInstance)
                .map(KDeployment.class::cast)
                .map(KDeployment::pod)
                .forEach(this::updateServiceRelationships);
        } else if (resource instanceof KPod) {
            this.updateServiceRelationships((KPod) resource);
        }
        return resource;
    }

    /**
     * Updates service relationships for a given pod.
     * @param pod A pod
     */
    private void updateServiceRelationships(final KPod pod) {
        this.resources.stream()
            .filter(KService.class::isInstance)
            .map(KService.class::cast)
            .filter(service -> service.selector().entrySet().stream()
                .map(e -> e.getValue().equals(pod.labels().get(e.getKey())))
                .reduce(true, Boolean::logicalAnd))
            .forEach(service -> {
                service.pod(pod);
                pod.service(service);
            });
    }

    /**
     * Deep copies this specification into a new one.
     * @return A non-null object
     */
    public KSpecification copy() {
        final KSpecification other = new KSpecification();
        this.resources.stream()
            .map(KResource::copy)
            .forEach(other::add);
        return other;
    }

    /**
     * Adds a new resource to this specification.
     * @param resource The resource to add
     */
    public void add(final KResource resource) {
        this.resources.add(resource);
        this.updateRelationships(resource);
    }

    /**
     * Adds a new resource to this specification.
     * @param resource The resource to replace
     * @param replacement The new resource
     */
    public void replace(final KResource resource, final KResource replacement) {
        this.resources.set(this.resources.indexOf(resource), replacement);
        this.updateRelationships(replacement);
    }

    /**
     * Adds a new file.
     * @param file The file to add
     * @param content The content of the file
     */
    public void add(final File file, final String content) {
        this.files.put(file, content);
    }

    /**
     * The resources in this specification.
     * @return An non-null unmodifiable list
     */
    public List<KResource> resources() {
        return Collections.unmodifiableList(this.resources);
    }

    /**
     * Configuration files to generate relative to the YAML source file.
     * @return An non-null unmodifiable map
     */
    public Map<File, String> files() {
        return Collections.unmodifiableMap(this.files);
    }

    /**
     * Writes this specification and associated files to the target directory.
     * @param directory The target directory
     * @throws IOException If there is an I/O error
     */
    public void write(final File directory) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Files.write(
            new File(directory, "specification.yaml").toPath(),
            this.yaml().getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
        for (final Map.Entry<File, String> entry : this.files.entrySet()) {
            Files.write(
                new File(directory, entry.getKey().getPath()).toPath(),
                entry.getValue().getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    @Override
    public String yaml() {
        return Yaml.dumpAll(
            this.resources.stream()
                .map(KResource::model)
                .iterator()
        );
    }

    @Override
    public String toString() {
        return this.yaml();
    }

}
