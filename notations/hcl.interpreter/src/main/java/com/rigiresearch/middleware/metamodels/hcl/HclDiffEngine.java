package com.rigiresearch.middleware.metamodels.hcl;

import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.diff.FeatureFilter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

/**
 * A Diff engine tailored for the HCL model.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclDiffEngine extends DefaultDiffEngine {

    @Override
    protected FeatureFilter createFeatureFilter() {
        return new HclFeatureFilter();
    }

    /**
     * An HCL feature filter to prevent comments from being replaced.
     */
    private static final class HclFeatureFilter extends FeatureFilter {

        @Override
        protected boolean isIgnoredReference(final Match match,
            final EReference reference) {
            final boolean ignore;
            if (match.getLeft() == null || match.getRight() == null) {
                ignore = super.isIgnoredReference(match, reference);
            } else {
                ignore = HclFeatureFilter.handle(
                    reference,
                    match.getLeft()
                );
            }
            return ignore;
        }

        /**
         * Whether a feature reference should be ignored.
         * @param reference The feature reference
         * @param object The eObject
         * @return Always {@code false}
         */
        private static boolean handle(final EReference reference,
            final EObject object) {
            final boolean ignore;
            if (object instanceof Resource) {
                ignore = reference == HclPackage.Literals.RESOURCE__COMMENT
                    && ((Resource) object).getComment() != null;
            } else if (object instanceof NameValuePair) {
                ignore = reference == HclPackage.Literals.NAME_VALUE_PAIR__COMMENT
                    && ((NameValuePair) object).getComment() != null;
            } else {
                ignore = false;
            }
            return ignore;
        }

    }

}
