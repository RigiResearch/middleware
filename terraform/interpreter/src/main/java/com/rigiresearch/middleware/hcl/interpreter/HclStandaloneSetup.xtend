package com.rigiresearch.middleware.hcl.interpreter

import com.google.inject.Injector
import com.rigiresearch.middleware.hcl.model.ModelPackage
import org.eclipse.emf.ecore.EPackage

/**
 * Initialization support for running Xtext languages without Equinox extension
 * registry.
 * @author Miguel Jiménez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
class HclStandaloneSetup extends HclStandaloneSetupGenerated {

	/**
	 * Creates the language injector and performs EMF registration.
	 */
	def static void doSetup() {
		new HclStandaloneSetup().createInjectorAndDoEMFRegistration()
	}

	override register(Injector injector) {
		if (!EPackage.Registry.INSTANCE.containsKey(ModelPackage.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);
		}
		super.register(injector)
	}

}
