package com.rigiresearch.middleware.notations.hcl.runtime;

import com.rigiresearch.middleware.metamodels.hcl.HclMatchEngineQualifiedNameConverter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;

/**
 * A custom qualified name provider.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclQualifiedNameProvider
    extends DefaultDeclarativeQualifiedNameProvider {

    /**
     * A qualified name converter.
     */
    private final IQualifiedNameConverter xconverter;

    /**
     * An HCL qualified name converter.
     */
    private final HclMatchEngineQualifiedNameConverter converter;

    /**
     * Default constructor.
     */
    public HclQualifiedNameProvider() {
        super();
        this.xconverter = new IQualifiedNameConverter.DefaultImpl();
        this.converter = new HclMatchEngineQualifiedNameConverter();
    }

    @Override
    public QualifiedName getFullyQualifiedName(final EObject eobject) {
        final QualifiedName name;
        final String identifier = this.converter.fullyQualifiedName(eobject);
        if (identifier == null) {
            name = this.getOrComputeFullyQualifiedName(eobject);
        } else {
            name = this.xconverter.toQualifiedName(identifier);
        }
        return name;
    }

}
