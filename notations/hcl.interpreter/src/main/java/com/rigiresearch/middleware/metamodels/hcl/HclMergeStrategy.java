package com.rigiresearch.middleware.metamodels.hcl;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.utils.EMFComparePredicates;
import org.eclipse.emf.compare.utils.UseIdentifiers;

/**
 * A merge utility tailored for HCL specifications.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public final class HclMergeStrategy {

    /**
     * Factory ranking.
     */
    private static final int RANKING = 20;

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
            .setMatchEngineFactoryRegistry(HclMergeStrategy.matchFactory())
            .build()
            .compare(new DefaultComparisonScope(previous, current, null));
        final Iterable<Diff> filtered = comparison.getDifferences()
            .stream()
            .filter(this.predicate)
            .collect(Collectors.toList());
        this.merger.copyAllRightToLeft(filtered, new BasicMonitor());
        return previous;
    }

    /**
     * Instantiates the match engine's factory registry.
     * @return A non-null registry object
     */
    private static IMatchEngine.Factory.Registry matchFactory() {
        final HclMatchEngineQualifiedNameConverter converter =
            new HclMatchEngineQualifiedNameConverter();
        // Initialize the factory
        final MatchEngineFactoryImpl factory = new MatchEngineFactoryImpl(
            new IdentifierEObjectMatcher(
                DefaultMatchEngine
                    .createDefaultEObjectMatcher(UseIdentifiers.WHEN_AVAILABLE),
                converter::fullyQualifiedName
            ),
            new DefaultComparisonFactory(new DefaultEqualityHelperFactory())
        );
        // Initialize the registry
        final IMatchEngine.Factory.Registry registry =
            MatchEngineFactoryRegistryImpl.createStandaloneInstance();
        // default engine ranking is 10, must be higher to override.
        factory.setRanking(HclMergeStrategy.RANKING);
        registry.add(factory);
        return registry;
    }

}
