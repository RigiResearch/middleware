package com.rigiresearch.middleware.coordinator;

import com.google.common.base.Predicates;
import com.rigiresearch.middleware.metamodels.hcl.HclPackage;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.utils.EMFComparePredicates;

/**
 * A component for coordinating the specification evolution.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class EvolutionCoordination {

    /**
     * Merges two HCL models.
     * @param previous The previous version of the model
     * @param current The current version of the model
     * @return A non-null specification
     */
    public Specification merge(final Specification previous,
        final Specification current) {
        final Comparison comparison = EMFCompare.builder()
            .setDiffEngine(new HclDiffEngine())
            .build()
            .compare(new DefaultComparisonScope(previous, current, null));
        final Predicate<? super Diff> predicate = Predicates.and(
            // Do not replace the whole dictionary but only element by element
            Predicates.not(
                EMFComparePredicates.onFeature(
                    HclPackage.Literals.DICTIONARY__ELEMENTS
                )
            ),
            // Do not merge elements with some kind of conflict
            Predicates.not(
                EMFComparePredicates.hasConflict(
                    ConflictKind.REAL,
                    ConflictKind.PSEUDO
                )
            ),
            // Do not merge null elements
            diff -> diff.getMatch().getRight() != null
        );
        final Iterable<Diff> filtered = comparison.getDifferences()
            .stream()
            .filter(predicate)
            .collect(Collectors.toList());
        final IBatchMerger merger =
            new BatchMerger(IMerger.RegistryImpl.createStandaloneInstance());
        merger.copyAllRightToLeft(filtered, new BasicMonitor());
        return previous;
    }

}
