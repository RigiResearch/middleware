package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import lombok.NoArgsConstructor;

/**
 * Transformation to create an HCL model from collected data from vSphere.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@NoArgsConstructor
public final class Data2Hcl {

    /**
     * Transform the collected data into a {@link Specification}.
     * @param data The colected data
     * @return The specification instance
     */
    public Specification specification(final JsonNode data) {
        // TODO Transform the data into spec. Start from the VMs, then the
        //  resources with which they are related. When the "graph" is complete,
        //  take the attributes/elements that can be changed at run-time and
        //  instantiate the model. The evolution coordinator takes that partial
        //  model and will merge it into the one represented by the HCL
        //  specification.
        return HclFactory.eINSTANCE.createSpecification();
    }

}
