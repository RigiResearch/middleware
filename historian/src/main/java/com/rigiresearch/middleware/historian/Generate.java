package com.rigiresearch.middleware.historian;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.acceleo.engine.service.AbstractAcceleoGenerator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

/**
 * The Acceleo generator for the {@code Generate} template.
 * @author Miguel Jimenez (miguel@leslumier.es)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings({
    "PMD.OnlyOneConstructorShouldDoInitialization",
    "PMD.ConstructorOnlyInitializesOrCallOtherConstructors"
})
public final class Generate extends AbstractAcceleoGenerator {

    /**
     * The name of the module.
     */
    private static final String MODULE_FILE_NAME = "/acceleo/Generate";

    /**
     * The name of the templates that are to be generated.
     */
    private static final String[] TEMPLATE_NAMES = {"main"};

    /**
     * This allows clients to instantiates a generator with all required information.
     *
     * @param uri The URI where the input model is located.
     * @param target The output folder for generating the files
     * @throws IOException If the module cannot be found, it cannot be loaded,
     *  or the model cannot be loaded.
     */
    public Generate(final URI uri, final File target) throws IOException {
        super();
        this.initialize(uri, target, Collections.emptyList());
    }

    /**
     * This allows clients to instantiates a generator with all required information.
     *
     * @param instance The input model.
     * @param target The output folder for generating the files
     * @throws IOException If the module cannot be found, it cannot be loaded,
     *  or the model cannot be loaded.
     */
    public Generate(final EObject instance, final File target) throws IOException {
        super();
        this.initialize(instance, target, Collections.emptyList());
    }

    @Override
    public String getModuleName() {
        return Generate.MODULE_FILE_NAME;
    }

    @Override
    public String[] getTemplateNames() {
        return Generate.TEMPLATE_NAMES;
    }

}
