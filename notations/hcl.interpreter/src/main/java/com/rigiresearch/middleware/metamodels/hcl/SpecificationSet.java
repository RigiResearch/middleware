package com.rigiresearch.middleware.metamodels.hcl;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import com.rigiresearch.middleware.notations.hcl.runtime.HclQualifiedNameProvider;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Specification set for HCL source code coming from separate files.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class SpecificationSet {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SpecificationSet.class);

    /**
     * Default size for collections/maps.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * The specifications composing this set.
     */
    private final List<Specification> elements;

    /**
     * A fqn-(resource-spec) mapping to recreate the source code files.
     */
    private final Map<QualifiedName, Map.Entry<Resource, Specification>> mapping;

    /**
     * A qualified name provider for HCL resources.
     */
    private final IQualifiedNameProvider provider;

    /**
     * An HCL parser.
     */
    private final HclParser parser;

    /**
     * Default constructor.
     * @param specifications The specifications composing this set
     */
    public SpecificationSet(final Specification... specifications) {
        this.elements = Arrays.stream(specifications)
            .collect(Collectors.toCollection(ArrayList::new));
        this.provider = new HclQualifiedNameProvider();
        this.mapping = this.initializeMapping();
        this.parser = new HclParser();
    }

    /**
     * Initializes the resource mapping.
     * @return A non-null map
     */
    private Map<QualifiedName, Map.Entry<Resource, Specification>> initializeMapping() {
        final Map<QualifiedName, Map.Entry<Resource, Specification>> map =
            new HashMap<>(SpecificationSet.INITIAL_CAPACITY);
        for (final Specification spec : this.elements) {
            for (final Resource resource : spec.getResources()) {
                map.put(
                    this.provider.getFullyQualifiedName(resource),
                    new AbstractMap.SimpleEntry<>(resource, spec)
                );
            }
        }
        return map;
    }

    /**
     * A unified specification composed of resources coming from this set's
     * specifications.
     * @return A non-null specification
     */
    public Specification unified() {
        // May throw DanglingHREFException if serialized, but it's better than null
        // See HclParser#parse(Resource)
        Specification specification = HclFactory.eINSTANCE.createSpecification();
        try {
            specification = this.parser.parse("");
        } catch (final HclParsingException | IOException exception) {
            SpecificationSet.LOGGER.error(
                "Error creating empty specification",
                exception
            );
        }
        for (final Specification spec : this.elements) {
            specification.getResources().addAll(spec.getResources());
        }
        return specification;
    }

    /**
     * A map of URI-specification elements.
     * Note: Each specification must have a unique URI.
     * @return A non-null map
     */
    public Map<URI, Specification> getMapping() {
        final Map<URI, Specification> map =
            new HashMap<>(SpecificationSet.INITIAL_CAPACITY);
        for (final Specification specification : this.elements) {
            if (map.containsKey(specification.eResource().getURI())) {
                throw new IllegalStateException("URI must be unique per specification");
            } else {
                map.put(specification.eResource().getURI(), specification);
            }
        }
        return map;
    }

    /**
     * Updates existing resources in this set and adds new ones.
     * New resources are added according to the following rules:
     * <ul>
     *     <li>If there's only one file, new resources are associated with it.</li>
     *     <li>If there's a file for each specifier, new resources are associated
     *     with its corresponding file</li>
     *     <li>Else, new resources are associated with the file URI "main.tf".
     *     If that URI doesn't exist, one is created.</li>
     * </ul>
     * <b>Note</b>: If a resource is not in the updated specification, it is
     * removed from this set.
     * @param specification An updated unified specification
     */
    public void update(final Specification specification) {
        final Map<URI, Specification> specs = this.getMapping();
        // 1. Delete resources
        this.removeResources(specification);
        // 2. Update and add resources
        final boolean specifiers =
            SpecificationSet.isOrganizedBasedOnSpecifiers(specs);
        for (final Resource tmp : specification.getResources()) {
            final QualifiedName fqn = this.provider.getFullyQualifiedName(tmp);
            if (this.mapping.containsKey(fqn)) {
                final Map.Entry<Resource, Specification> entry = this.mapping.get(fqn);
                // Replace the old resource with the new one
                entry.getValue().getResources().remove(entry.getKey());
                entry.getValue().getResources().add(EcoreUtil.copy(tmp));
            } else {
                // New resource
                if (specs.size() == 1) {
                    final Specification spec = specs.values().iterator().next();
                    spec.getResources().add(EcoreUtil.copy(tmp));
                    this.mapping.put(fqn, new AbstractMap.SimpleEntry<>(tmp, spec));
                } else if (specifiers && !"resource".equals(tmp.getSpecifier())) {
                    final String name = String.format("%s.tf", tmp.getSpecifier());
                    this.addToCorrespondingUri(URI.createFileURI(name), tmp, specs);
                } else {
                    this.addToCorrespondingUri(URI.createFileURI("main.tf"), tmp, specs);
                }
            }
        }
    }

    /**
     * Removes resources from this set (including empty specifications) that are
     * not in the given specification.
     * @param specification The speficiation
     */
    private void removeResources(final Specification specification) {
        final Iterator<QualifiedName> iterator = this.mapping.keySet().iterator();
        while (iterator.hasNext()) {
            final QualifiedName fqn = iterator.next();
            boolean found = false;
            for (final Resource tmp : specification.getResources()) {
                if (fqn.equals(this.provider.getFullyQualifiedName(tmp))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Remove the specification if that was the only resource in it
                final Specification spec = this.mapping.get(fqn).getValue();
                if (spec.getResources().size() == 1) {
                    this.elements.remove(spec);
                } else {
                    spec.getResources()
                        .removeIf(
                            tmp -> fqn.equals(this.provider.getFullyQualifiedName(tmp))
                        );
                }
                // Remove the resource from the mapping
                iterator.remove();
            }
        }
    }

    /**
     * Adds the given resource to an existing specification or creates a new one.
     * @param uri The expected URI associated with the specification
     * @param resource The resource to add
     * @param specifications A URI-specification mapping
     */
    private void addToCorrespondingUri(final URI uri, final Resource resource,
        final Map<URI, Specification> specifications) {
        final QualifiedName fqn = this.provider.getFullyQualifiedName(resource);
        final Optional<URI> optional = specifications.keySet()
            .stream()
            .filter(tmp -> tmp.toFileString().endsWith(uri.lastSegment()))
            .findAny();
        Specification spec = null;
        if (optional.isPresent()) {
            spec = specifications.get(optional.get());
        } else {
            try {
                spec = this.parser.parse(this.parser.resource(uri));
            } catch (final HclParsingException exception) {
                // This shouldn't happen
                SpecificationSet.LOGGER.error(
                    "Error parsing empty resource",
                    exception
                );
            }
            this.elements.add(spec);
        }
        spec.getResources().add(EcoreUtil.copy(resource));
        this.mapping.put(fqn, new AbstractMap.SimpleEntry<>(resource, spec));
    }

    /**
     * Determines whether a set of HCL files conform to the resource specifiers.
     * @param specifications The file specifications
     * @return True if all the file names contain the specifier of all the
     *  resources they contain, False otherwise
     */
    private static boolean isOrganizedBasedOnSpecifiers(
        final Map<URI, Specification> specifications) {
        boolean specifiers = true;
        loop: for (final Map.Entry<URI, Specification> entry
            : specifications.entrySet()) {
            final String segment = entry.getKey().lastSegment();
            final String name = segment.substring(0, segment.lastIndexOf('.'));
            for (final Resource resource : entry.getValue().getResources()) {
                // e.g., variable, variables, vm_variables, etc.
                if (name.contains(resource.getSpecifier())) {
                    specifiers = false;
                    break loop;
                }
            }
        }
        return specifiers;
    }

}
