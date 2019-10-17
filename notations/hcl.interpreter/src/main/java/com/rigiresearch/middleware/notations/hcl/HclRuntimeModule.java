package com.rigiresearch.middleware.notations.hcl;

import com.rigiresearch.middleware.notations.hcl.runtime.HclQualifiedNameProvider;
import com.rigiresearch.middleware.notations.hcl.runtime.HclValueConverterService;
import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

/**
 * Use this class to register components to be used at runtime / without the
 * Equinox extension registry.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class HclRuntimeModule extends AbstractHclRuntimeModule {

    /**
     * Binds a custom qualified name provider.
     * @return A class
     */
    @Override
    public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
        return HclQualifiedNameProvider.class;
    }

    /**
     * Binds a custom value converter.
     * @return A class
     */
    @Override
    public Class<? extends IValueConverterService> bindIValueConverterService() {
        return HclValueConverterService.class;
    }

}
