package com.rigiresearch.middleware.notations.hcl.runtime;

import com.rigiresearch.middleware.metamodels.hcl.Resource;
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
    private final IQualifiedNameConverter converter;

    /**
     * Default constructor.
     */
    public HclQualifiedNameProvider() {
        super();
        this.converter = new IQualifiedNameConverter.DefaultImpl();
    }

    @Override
    public QualifiedName getFullyQualifiedName(final EObject eobject) {
        final QualifiedName name;
        if (eobject instanceof Resource) {
            final Resource resource = (Resource) eobject;
            name = this.qualifiedName(
                resource.getSpecifier(),
                resource.getType(),
                resource.getName()
            );
        } else {
            name = this.getOrComputeFullyQualifiedName(eobject);
        }
        return name;
    }

    /**
     * Creates a qualified name our of various components.
     * Skips null components.
     * @param components The parts of the qualified name
     * @return A non-null qualified name
     */
    private QualifiedName qualifiedName(final String... components) {
        final String separator = ".";
        final StringBuilder builder = new StringBuilder(components.length);
        for (int position = 0; position < components.length; position += 1) {
            if (components[position] != null) {
                builder.append(components[position]);
                if (position < components.length - 1) {
                    builder.append(separator);
                }
            }
        }
        return this.converter.toQualifiedName(builder.toString());
    }

}
