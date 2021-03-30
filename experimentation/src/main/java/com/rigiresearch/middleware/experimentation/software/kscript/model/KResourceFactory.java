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
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1Service;

/**
 * A resource factory.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class KResourceFactory {

    /**
     * Instantiates a resource based on the given Kubernetes object.
     * @param object The Kubernetes object
     * @return A non-null resource
     */
    public KResource resource(final Object object) {
        final KResource instance;
        if (object instanceof V1Deployment) {
            instance = new KDeployment((V1Deployment) object);
        } else if (object instanceof V1Pod) {
            instance = new KPod((V1Pod) object);
        } else if (object instanceof V1Service) {
            instance = new KService((V1Service) object);
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported object type %s", object.getClass())
            );
        }
        return instance;
    }

}
