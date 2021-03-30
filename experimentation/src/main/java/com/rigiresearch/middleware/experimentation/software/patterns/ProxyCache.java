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
package com.rigiresearch.middleware.experimentation.software.patterns;

import com.rigiresearch.middleware.experimentation.software.kscript.KScript;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KContainer;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KSpecification;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;

/**
 * A proxy cache pattern that can be applied either on containers using a particular image or
 * containers in a pod with certain labels. The configuration options are:
 * <ul>
 *     <li>{@code image}: the name of the container image (may include the version).
 *     The expected object is a String. Examples: nginx, nginx:latest</li>
 *     <li>{@code labels}: the labels associated with the target containers.
 *     The expected object is a Map. Example: {app: myapp}</li>
 * </ul>
 * <b>Note:</b> If both {@code image} and {@code labels} are provided, the pattern will be applied
 * on containers that hold both conditions.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@NoArgsConstructor
public final class ProxyCache extends AbstractPattern {

    /**
     * The proxy implementation to use.
     */
    private static final String IMAGE = "nginx:latest";

    /**
     * The name of the proxy container container.
     */
    private static final String NAME = "proxy_cache";

    @Override
    public KSpecification apply(final KSpecification spec,
        final Map<String, Object> config) {
        final KSpecification transformed = spec.copy();
        final KScript ks = new KScript(spec);
        final List<KContainer> containers = this.containers(config, transformed);
        final KContainer proxy = new KContainer(
            ProxyCache.NAME,
            ProxyCache.IMAGE,
            Collections.singletonList(ks.randomPort())
        );
        containers.forEach(container -> container.pod().add(proxy.copy()));
        // TODO Update the original container's port
        return transformed;
    }

}
