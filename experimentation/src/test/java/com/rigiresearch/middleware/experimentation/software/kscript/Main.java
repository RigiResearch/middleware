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

import com.google.common.io.Resources;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KSpecification;
import com.rigiresearch.middleware.experimentation.software.patterns.LoadBalancer;
import com.rigiresearch.middleware.experimentation.software.patterns.Pattern;
import com.rigiresearch.middleware.experimentation.software.patterns.ProxyCache;
import io.kubernetes.client.util.Yaml;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A test class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class Main {

    /**
     * Initial collection capacity.
     */
    private static final int INITIAL_CAPACITY = 10;

    @Test
    void testItRuns() throws IOException {
        // Load the sample file
        final KSpecification original = new KSpecification(
            Yaml.loadAll(
                Resources.toString(
                    Resources.getResource("demo/encoder.yaml"),
                    StandardCharsets.UTF_8
                )
            )
        );
        // Input parameters
        final Map<String, String> labels = new HashMap<>(Main.INITIAL_CAPACITY);
        labels.put("app", "encoder");
        final Map<String, Object> options = new HashMap<>(Main.INITIAL_CAPACITY);
        options.put("image", "jachinte/encoder-rest");
        options.put("labels", labels);

        // Apply a proxy cache
        final Pattern cache = new ProxyCache();
        final KSpecification transformed = cache.apply(original, options);

        // Apply a load balancer
        final Pattern balancer = new LoadBalancer();
        options.put("replicas", 15);
        balancer.apply(transformed, options)
            .write(new File("/tmp"));
    }

}
