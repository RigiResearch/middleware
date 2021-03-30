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
import com.rigiresearch.middleware.experimentation.software.kscript.model.KDeployment;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KPod;
import com.rigiresearch.middleware.experimentation.software.kscript.model.KSpecification;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link KSpecification}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
class KSpecificationTest {

    @Test
    void testExportingAndImportingSimpleSpecification() throws IOException {
        final KSpecification specification = new KSpecification();
        final KPod pod = new KPod("coffee-v2-pod", new HashMap<>(0), new ArrayList<>(0));
        specification.add(new KDeployment("coffee-v2", 1, pod));
        final List<Object> objects = Yaml.loadAll(specification.yaml());
        Assertions.assertEquals(
            specification.yaml(),
            new KSpecification(objects).yaml(),
            "The YAML-formatted string is different"
        );
    }

    @Test
    void initSpecFromFile() throws IOException {
        final KSpecification specification = new KSpecification(
            Yaml.loadAll(
                Resources.toString(
                    Resources.getResource("cafe.yaml"),
                    StandardCharsets.UTF_8
                )
            )
        );
        Assertions.assertNotNull(
            specification,
            "Unknown error loading specification"
        );
    }

}
