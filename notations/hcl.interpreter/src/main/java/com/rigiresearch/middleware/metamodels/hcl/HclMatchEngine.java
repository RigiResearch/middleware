package com.rigiresearch.middleware.metamodels.hcl;

import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;

/**
 * A custom match engine for HCL.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclMatchEngine extends DefaultMatchEngine {

    /**
     * Default constructor.
     * @param matcher The matcher object
     * @param factory The comparison factory
     */
    public HclMatchEngine(final IEObjectMatcher matcher,
        final IComparisonFactory factory) {
        super(matcher, factory);
    }

}
