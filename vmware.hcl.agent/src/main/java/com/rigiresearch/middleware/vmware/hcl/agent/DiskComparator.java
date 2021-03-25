package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;

/**
 * A VM disk comparator to sort y key.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class DiskComparator implements Comparator<JsonNode> {

    /**
     * An attribute used for comparison.
     */
    private static final String KEY = "/key";

    @Override
    public int compare(final JsonNode first, final JsonNode second) {
        return Integer.valueOf(first.at(DiskComparator.KEY).textValue())
            .compareTo(Integer.valueOf(second.at(DiskComparator.KEY).textValue()));
    }

}
