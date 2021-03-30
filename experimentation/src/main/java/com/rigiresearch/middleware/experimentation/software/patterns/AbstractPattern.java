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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * An abstract pattern with the basic plumbing for creating patterns.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPattern implements Pattern {

    /**
     * Finds containers that match a particular image name or containers whose pod matches a
     * collection of labels.
     * @param config The pattern's options values
     * @param spec The specification
     * @return A list of containers that matched the image and/or labels
     */
    protected List<KContainer> containers(final Map<String, Object> config,
        final KSpecification spec) {
        final Optional<String> image = this.option(config, "image");
        final Optional<Map<String, String>> labels = this.option(config, "labels");
        final KScript ks = new KScript(spec);
        final List<KContainer> containers;
        if (image.isPresent() && labels.isPresent()) {
            containers = ks.containers(
                ks.services(labels.get()),
                c -> c.image().startsWith(image.get())
            );
        } else if (image.isPresent() && !labels.isPresent()) {
            containers = ks.containers(c -> c.image().startsWith(image.get()));
        } else if (!image.isPresent() && labels.isPresent()) {
            containers = ks.containers(ks.services(labels.get()));
        } else {
            throw new IllegalArgumentException("Either 'image' or 'labels' is expected");
        }
        return containers;
    }

    /**
     * Gets the value for a particular option. Example of use:
     * <pre>Integer replicas = Pattern.<Integer>option(config, "replicas").orElse(3);</pre>
     * @param config The config values (may be empty)
     * @param key The option key
     * @param <T> The expected return type
     */
    protected <T> Optional<T> option(final Map<String, Object> config, final String key) {
        return Optional.ofNullable((T) config.get(key));
    }

}
