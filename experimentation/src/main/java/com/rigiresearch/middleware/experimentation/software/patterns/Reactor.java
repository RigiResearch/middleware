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

import com.rigiresearch.middleware.experimentation.software.kscript.model.KSpecification;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TODO document this class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class Reactor extends AbstractPattern {

    @Override
    public KSpecification apply(final KSpecification spec,
        final Map<String, Object> config) {
        final KSpecification transformed = spec.copy();
        final Optional<List<String>> paths = this.option(config, "paths");
        if (paths.isPresent()) {
            // TODO Add support for VirtualServer
            // Example: https://github.com/nginxinc/kubernetes-ingress/blob/master/examples-of-custom-resources/advanced-routing/cafe-virtual-server.yaml
        } else {
            throw new IllegalArgumentException("Argument 'paths' is mandatory");
        }
        return transformed;
    }

}
