package com.rigiresearch.middleware.notations.hcl.generator;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.AbstractGenerator;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.generator.IGeneratorContext;

/**
 * A code generator.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class HclGenerator extends AbstractGenerator {

    /**
     * Generates output text based on the given resource.
     * @param resource The resource to transform
     * @param fsa The file system interface
     * @param context Generator context
     */
    @Override
    public void doGenerate(final Resource resource,
        final IFileSystemAccess2 fsa, final IGeneratorContext context) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
