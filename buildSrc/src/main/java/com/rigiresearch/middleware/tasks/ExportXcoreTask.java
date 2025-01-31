package com.rigiresearch.middleware.tasks;

import com.google.inject.Injector;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup;
import org.eclipse.emf.mwe.utils.StandaloneSetup;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Exports the .ecore and .genmodel files for a Xcore model.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public class ExportXcoreTask extends DefaultTask {

    /**
     * The file path for the input .xcore file.
     */
    public File xcoreInputFile;

    /**
     * The file path for the .ecore file.
     */
    public File ecoreOutputFile;

    /**
     * The file path for the .genmodel file.
     */
    public File genmodelOutputFile;

    /**
     * Default constructor.
     */
    public ExportXcoreTask() {
        super();
        this.setGroup("Modeling");
        this.setDescription(
            "Exports the .ecore and .genmodel files for an Xcore model");
    }

    /**
     * Generates the .ecore and .genmodel files for the specified Xcore model.
     * @throws IOException If saving the file fails
     */
    @TaskAction
    public final void generate() throws IOException {
        new StandaloneSetup().setPlatformUri("./");
        final Injector injector = new XcoreStandaloneSetup()
            .createInjectorAndDoEMFRegistration();
        final XtextResourceSet set =
            injector.getInstance(XtextResourceSet.class);
        set.addLoadOption(
            XtextResource.OPTION_RESOLVE_ALL,
            Boolean.TRUE
        );
        final URI uri =
            URI.createFileURI(this.xcoreInputFile.getAbsolutePath());
        final ResourceSet rset = new ResourceSetImpl();
        rset.getURIConverter()
            .getURIMap()
            .putAll(EcorePlugin.computePlatformURIMap(true));
        final Resource input = rset.getResource(uri, true);
        input.load(Collections.EMPTY_MAP);
        EcoreUtil.resolveAll(input);
        ExportXcoreTask.save(
            input,
            this.ecoreOutputFile,
            EcorePackage.Literals.EPACKAGE
        );
        ExportXcoreTask.save(
            input,
            this.genmodelOutputFile,
            GenModelPackage.Literals.GEN_MODEL
        );
    }

    /**
     * Creates and saves the resource corresponding to the specified literal.
     * @param input The input resource (the Xcore model)
     * @param file The output file (e.g., /.../model/MyModel.ecore)
     * @param literal The literal type (e.g., EcorePackage.Literals.EPACKAGE)
     * @throws IOException If saving the file fails
     */
    private static void save(final Resource input, final File file,
        final EClass literal) throws IOException {
        final URI uri = URI.createFileURI(file.getAbsolutePath());
        final ResourceSet set = input.getResourceSet();
        final Resource resource = set.createResource(uri);
        final EObject object = (EObject) EcoreUtil.getObjectByType(
            input.getContents(),
            literal
        );
        resource.getContents().add(object);
        resource.save(Collections.EMPTY_MAP);
    }

    /**
     * Gets the Xcore input file.
     * @return A file
     */
    @InputFile
    public final File getXcoreInputFile() {
        return this.xcoreInputFile;
    }

    /**
     * Gets the Ecore output file.
     * @return A file
     */
    @OutputFile
    public final File getEcoreOutputFile() {
        return this.ecoreOutputFile;
    }

    /**
     * Gets the Genmodel output file.
     * @return A file
     */
    @OutputFile
    public final File getGenmodelOutputFile() {
        return this.genmodelOutputFile;
    }

}
