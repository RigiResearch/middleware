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
package com.rigiresearch.middleware.experimentation.software.kscript;

import com.rigiresearch.middleware.experimentation.software.kscript.model.KContainer;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KDeployment;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KService;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * A main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class KScript {

    /**
     * The specification on which the commands will be applied.
     */
    private final KSpecification specification;

    public KContainer container(final Predicate<KContainer> predicate) {
        return this.containers(predicate).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("KContainer does not exist"));
    }

    public List<KContainer> containers(final Predicate<KContainer> predicate) {
        final List<KContainer> containers = new ArrayList<>();
        containers.addAll(
            this.specification.resources().stream()
                .filter(KDeployment.class::isInstance)
                .map(KDeployment.class::cast)
                .map(KDeployment::pod)
                .flatMap(pod -> pod.containers().stream())
                .filter(predicate::test)
                .collect(Collectors.toList())
        );
        return containers;
    }

    public List<KContainer> containers(final List<KService> services) {
        return this.containers(services, c -> true);
    }

    // Return the containers in a list of services
    public List<KContainer> containers(final List<KService> services,
        final Predicate<KContainer> predicate) {
        return services.stream()
            .map(KService::pod)
            .flatMap(pod -> pod.containers().stream())
            .filter(predicate)
            .collect(Collectors.toList());
    }

    public KService service(final String name) {
        return this.specification.resources().stream()
            .filter(KService.class::isInstance)
            .map(KService.class::cast)
            .filter(service -> service.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Service does not exist"));
    }

    // Assumes that pods are always deployed within a service
    public List<KService> services(final Map<String, String> labels) {
        return this.specification.resources()
            .stream()
            .filter(KService.class::isInstance)
            .map(KService.class::cast)
            .filter(service -> service.selector().entrySet().containsAll(labels.entrySet()))
            .collect(Collectors.toList());
    }

    public KDeployment deployment(final String name) {
        return this.specification.resources().stream()
            .filter(KDeployment.class::isInstance)
            .map(KDeployment.class::cast)
            .filter(deployment -> deployment.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Deployment does not exist"));
    }

    /**
     * Generates a random port that is not in the given array.
     * @param ports The already bound ports
     * @return A new valid port
     */
    public int randomPort(final int... ports) {
        // TODO Implement the randomPort method
        return 8080;
    }

}
