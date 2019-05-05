package com.rigiresearch.middleware.notations.hcl;

import com.google.inject.Injector;
import com.rigiresearch.middleware.notations.metamodels.hcl.HclPackage;
import org.eclipse.emf.ecore.EPackage;

/**
 * Initialization support for running Xtext languages without Equinox extension
 * registry.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class HclStandaloneSetup extends HclStandaloneSetupGenerated {

    /**
     * Creates the language injector and performs EMF registration.
     */
    protected static void doSetup() {
        new HclStandaloneSetup().createInjectorAndDoEMFRegistration();
    }

    /**
     * Registers the HCL model package.
     * @param injector The injector
     */
    @Override
    public void register(final Injector injector) {
        if (!EPackage.Registry.INSTANCE.containsKey(HclPackage.eNS_URI)) {
            EPackage.Registry.INSTANCE.put(
                HclPackage.eNS_URI,
                HclPackage.eINSTANCE
            );
        }
        super.register(injector);
    }

}
