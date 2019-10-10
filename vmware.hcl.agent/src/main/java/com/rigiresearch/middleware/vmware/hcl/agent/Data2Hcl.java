package com.rigiresearch.middleware.vmware.hcl.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.rigiresearch.middleware.metamodels.hcl.Dictionary;
import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
import com.rigiresearch.middleware.metamodels.hcl.NameValuePair;
import com.rigiresearch.middleware.metamodels.hcl.Resource;
import com.rigiresearch.middleware.metamodels.hcl.ResourceReference;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.metamodels.hcl.Text;
import com.rigiresearch.middleware.metamodels.hcl.TextExpression;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Transformation to create an HCL model from collected data from vSphere.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Data2Hcl {

    /**
     * Initial capacity for lists and maps.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * The collected data.
     */
    private final JsonNode data;

    /**
     * The specification to populate.
     */
    private final Specification spec;

    /**
     * Variable-value map.
     * These values will be passed to the evolution coordinator for comparison
     * with current values.
     */
    private final Map<String, String> values;

    /**
     * Number of instances for a specific resource type and name.
     * The structure of this map is as follows:
     * &lt;resource-specifier, &lt;name, number of instances&gt;&gt;
     */
    private final Map<String, Map<String, Integer>> instances;

    /**
     * Default constructor.
     * @param data The collected data
     */
    public Data2Hcl(final JsonNode data) {
        this.data = data;
        this.spec = HclFactory.eINSTANCE.createSpecification();
        this.values = new HashMap<>(Data2Hcl.INITIAL_CAPACITY);
        this.instances = new HashMap<>(Data2Hcl.INITIAL_CAPACITY);
    }

    /**
     * The current values for the template variables.
     * @return A non-null map
     */
    public Map<String, String> variableValues() {
        return this.values;
    }

    /**
     * Transform the collected data into a {@link Specification}.
     * @return The specification instance
     */
    public Specification specification() {
        // TODO Transform the data into spec. Start from the VMs, then the
        //  resources with which they are related. When the "graph" is complete,
        //  take the attributes/elements that can be changed at run-time and
        //  instantiate the model. The evolution coordinator takes that partial
        //  model and will merge it into the one represented by the HCL
        //  specification.
        this.data.get("getVcenterVm").forEach(this::createVm);
        return this.spec;
    }

    /**
     * Finds the group that contains a particular element. For example, given:
     * <pre>
     * {
     *     "listVcenterVmFilteredByDatacenter": {
     *         "datacenter-1": [
     *             "vm-1",
     *             "vm-2"
     *         ]
     *     }
     * }
     * </pre>
     * And these arguments: ("listVcenterVmFilteredByDatacenter", "vm-1"), this
     * method returns "datacenter-1". Note: This method assumes that every group
     * contains a list of strings and nothing else.
     * @param property The json property
     * @param element The element of interest
     * @return The group name (i.e., the containing element)
     */
    private Optional<String> findElementInGroupedSet(
        final String property, final String element) {
        String group = null;
        final JsonNode node = this.data.get(property);
        final Iterator<String> iterator = node.fieldNames();
        loop: while (iterator.hasNext()) {
            final String field = iterator.next();
            for (final JsonNode temp : node.get(field)) {
                if (temp.textValue().equals(element)) {
                    group = field;
                    break loop;
                }
            }
        }
        return Optional.ofNullable(group);
    }

    /**
     * Creates a VM resource.
     * @param node The JSON node that represents the VM resource
     */
    private void createVm(final JsonNode node) {
        final JsonNode value = node.get("value");
        // The vm attributes
        final Dictionary attributes = HclFactory.eINSTANCE.createDictionary();
        final Resource name =
            this.variable(attributes, "vm_%d_name", "name", "string");
        this.values.put(name.getName(), node.get("vm").asText());
        final Resource folder =
            this.variable(attributes, "vm_%d_folder", "folder", "string");
        this.values.put(
            folder.getName(),
            this.findElementInGroupedSet(
                "listVcenterVmFilteredByFolder",
                node.get("vm").asText()
            ).get()
        );
        final Resource cpus =
            this.variable(attributes, "vm_%s_number_of_vcpu", "num_cpus", "integer");
        this.values.put(cpus.getName(), value.get("cpu").get("count").asText());
        final Resource memory =
            this.variable(attributes, "vm_%s_memory", "memory", "integer");
        this.values.put(memory.getName(), value.get("memory").get("size_MiB").asText());
        // TODO disks
        // TODO resource pool
        // TODO datastore
        // TODO guest_id (changes? from where should I get it?)
        // TODO scsi_type (changes?)
        // TODO network
        // The vm
        final Resource resource = HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("resource");
        resource.setType("vsphere_virtual_machine");
        resource.setName(this.reserveName("resource", "vm_%d"));
        resource.setValue(attributes);
        this.spec.getResources().add(resource);
    }

    /**
     * Creates and adds a resource of type variable.
     * @param attributes The vm attributes
     * @param name The name of the variable
     * @param attribute The name of the vm attribute
     * @param type The type of variable
     * @return A non-null resource object
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private Resource variable(final Dictionary attributes,
        final String name, final String attribute, final String type) {
        // The template variable
        final Resource resource = HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("variable");
        resource.setName(this.reserveName("variable", name));
        final Dictionary value = HclFactory.eINSTANCE.createDictionary();
        final NameValuePair typep = HclFactory.eINSTANCE.createNameValuePair();
        final Text text = HclFactory.eINSTANCE.createText();
        text.setValue(type);
        typep.setName("type");
        typep.setValue(text);
        value.getElements().add(typep);
        resource.setValue(value);
        this.spec.getResources().add(resource);
        // The vm value
        final NameValuePair pair = HclFactory.eINSTANCE.createNameValuePair();
        pair.setName(attribute);
        pair.setValue(this.reference("var", resource.getName()));
        attributes.getElements().add(pair);
        return resource;
    }

    /**
     * Creates a resource reference.
     * @param components The fully qualified name of the referenced resource
     * @return A non-null text expression
     */
    private TextExpression reference(final String... components) {
        final ResourceReference resource = HclFactory.eINSTANCE.createResourceReference();
        Arrays.asList(components)
            .forEach(component -> resource.getFullyQualifiedName().add(component));
        final TextExpression exp = HclFactory.eINSTANCE.createTextExpression();
        exp.setBefore("${");
        exp.setReference(resource);
        exp.setAfter("}");
        return exp;
    }

    /**
     * Reserves a resource name based on the number of instances with that same
     * name.
     * @param specifier The resource specifier.
     * @param name The resource name format (including %d somewhere).
     * @return A non-null reserved name
     */
    private String reserveName(final String specifier, final String name) {
        if (!this.instances.containsKey(specifier)) {
            this.instances.put(specifier, new HashMap<>(Data2Hcl.INITIAL_CAPACITY));
        }
        final int number;
        if (this.instances.get(specifier).containsKey(name)) {
            number = this.instances.get(specifier).get(name) + 1;
        } else {
            number = 1;
        }
        this.instances.get(specifier).put(name, number);
        return String.format(name, number);
    }

}
