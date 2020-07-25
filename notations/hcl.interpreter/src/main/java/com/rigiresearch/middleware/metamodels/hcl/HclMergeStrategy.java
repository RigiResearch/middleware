package com.rigiresearch.middleware.metamodels.hcl;

import com.google.common.base.Predicates;
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
 * A merge utility tailored for HCL specifications.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclMergeStrategy {

    /**
     * A predicate to filter diff elements.
     */
    private final Predicate<? super Diff> predicate;

    /**
     * A batch merger.
     */
    private final IBatchMerger merger;

    /**
     * Default constructor.
     */
    public HclMergeStrategy() {
        this.predicate = Predicates.and(
            // Do not replace the whole specification but only resource by resource
            Predicates.not(
                EMFComparePredicates.onFeature(
                    HclPackage.Literals.SPECIFICATION__RESOURCES
                )
            ),
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
        this.merger = new BatchMerger(
            IMerger.RegistryImpl.createStandaloneInstance()
        );
    }

    /**
     * Merges two HCL models.
     * @param previous The previous version of the model
     * @param current The current version of the model
     * @return The previous specification containing all of the changes
     */
    public Specification merge(final Specification previous,
        final Specification current) {
        final Comparison comparison = EMFCompare.builder()
            .setDiffEngine(new HclDiffEngine())
            .build()
            .compare(new DefaultComparisonScope(previous, current, null));
        final Iterable<Diff> filtered = comparison.getDifferences()
            .stream()
            .filter(this.predicate)
            .collect(Collectors.toList());
        this.merger.copyAllRightToLeft(filtered, new BasicMonitor());
        return previous;
    }

}
