package com.rigiresearch.middleware.notations.hcl.parsing;

import com.google.inject.Injector;
import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.notations.hcl.HclStandaloneSetup;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A standalone parser to create an instance of the HCL model based on a given
 * text source (i.e., the AST).
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclParser {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(HclParser.class);

    /**
     * The Xtext injector.
     */
    private final Injector injector;

    /**
     * A resource set.
     */
    private final XtextResourceSet resources;

    /**
     * A validator for Hcl specifications.
     */
    private final IResourceValidator validator;

    /**
     * Default constructor.
     */
    public HclParser() {
        this.injector = new HclStandaloneSetup()
            .createInjectorAndDoEMFRegistration();
        this.resources = this.initialize();
        this.validator = this.injector.getInstance(IResourceValidator.class);
    }

    /**
     * Initializes the Xtext result set.
     * @return The initialized resource set
     */
    private XtextResourceSet initialize() {
        final XtextResourceSet set = this.injector
            .getInstance(XtextResourceSet.class);
        set.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
        return set;
    }

    /**
     * Parses an HCL model and returns the corresponding source code.
     * @param specification The HCL model
     * @return A non-null text
     */
    public String parse(final Specification specification) {
        return new Hcl2Text().source(specification);
    }

    /**
     * Parses several HCL specifications and returns their AST.
     * @param files The specification files
     * @return A {@link Specification} object
     * @throws HclParsingException If there are any parsing errors
     * @throws IOException If an I/O problem occurs
     */
    public SpecificationSet parse(final File... files)
        throws HclParsingException, IOException {
        final Specification[] specifications = new Specification[files.length];
        for (int position = 0; position < files.length; position += 1) {
            specifications[position] = this.parse(files[position]);
        }
        return new SpecificationSet(specifications);
    }

    /**
     * Parses an HCL specification and returns the AST.
     * @param file The specification file
     * @return A {@link Specification} object
     * @throws HclParsingException If there are any parsing errors
     * @throws IOException If an I/O problem occurs
     */
    public Specification parse(final File file)
        throws HclParsingException, IOException {
        final Resource resource = this.resource(
            URI.createFileURI(file.getPath())
        );
        resource.load(
            Files.newInputStream(file.toPath()),
            Collections.emptyMap()
        );
        return this.parse(resource);
    }

    /**
     * Parses an HCL specification and returns the AST.
     * @param source The specification source
     * @return A {@link Specification} object
     * @throws HclParsingException If there are any parsing errors
     * @throws IOException If an I/O problem occurs
     */
    public Specification parse(final String source)
        throws HclParsingException, IOException {
        final Resource resource = this.resource(
            URI.createURI(
                String.format(
                    "temp-%d.tf",
                    new SecureRandom()
                        .nextInt(Integer.MAX_VALUE)
                )
            )
        );
        resource.load(
            new ByteArrayInputStream(source.getBytes()),
            Collections.emptyMap()
        );
        return this.parse(resource);
    }

    /**
     * Parses an HCL specification and returns the AST.
     * @param resource The associated resource
     * @return A {@link Specification} object
     * @throws HclParsingException If there are any parsing errors
     */
    public Specification parse(final Resource resource)
        throws HclParsingException {
        final List<Issue> issues = this.validator.validate(
            resource,
            CheckMode.ALL,
            CancelIndicator.NullImpl
        );
        final List<Issue> errors = issues.stream()
            .filter(issue -> Severity.ERROR == issue.getSeverity())
            .collect(Collectors.toList());
        if (!errors.isEmpty()) {
            issues.forEach(issue -> HclParser.LOGGER.error(issue.toString()));
            throw new HclParsingException("The specification couldn't be parsed");
        }
        final Specification specification;
        if (resource.getContents().isEmpty()) {
            specification = HclFactory.eINSTANCE.createSpecification();
            // Avoid exception "DanglingHREFException: The object '...' is not
            // contained in a resource"
            resource.getContents().add(specification);
        } else {
            specification = (Specification) resource.getContents().get(0);
        }
        return specification;
    }

    /**
     * Creates a temporal Xtext resource using the HCL resource set.
     * @param uri The resource URI
     * @return The resource instance
     */
    public Resource resource(final URI uri) {
        final Optional<Resource> optional = this.resources.getResources()
            .stream()
            .filter(res -> res.getURI().toFileString().equals(uri.toFileString()))
            .findFirst();
        final Resource resource = optional.orElseGet(
            () -> this.resources.createResource(uri)
        );
        resource.unload();
        return resource;
    }

}
