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
import java.util.Map;

/**
 * A pattern.
 * @author Felipe Rivera (rivera@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@FunctionalInterface
public interface Pattern {

    /**
     * Applys this pattern on the given specification.
     * @param spec The original software specification that describes the current state of the
     *     system
     * @param config The configuration details of this pattern
     * @return A new software specification that describes the new version of the system once the
     *     pattern is applied
     */
    KSpecification apply(KSpecification spec, Map<String, Object> config);

}
