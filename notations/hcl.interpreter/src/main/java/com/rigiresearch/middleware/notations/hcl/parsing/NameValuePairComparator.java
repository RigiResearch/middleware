package com.rigiresearch.middleware.notations.hcl.parsing;

import com.rigiresearch.middleware.metamodels.hcl.Dictionary;
import com.rigiresearch.middleware.metamodels.hcl.NameValuePair;
import java.io.Serializable;
import java.util.Comparator;

/**
 * A name-value comparator.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class NameValuePairComparator
    implements Comparator<NameValuePair>, Serializable {

    @Override
    public int compare(final NameValuePair first, final NameValuePair second) {
        final int order;
        final boolean fdict = first.getValue() instanceof Dictionary;
        final boolean sdict = second.getValue() instanceof Dictionary;
        if (fdict && !sdict) {
            order = 1;
        } else if (!fdict && sdict) {
            order = -1;
        } else {
            order = first.getName().compareTo(second.getName());
        }
        return order;
    }

}
