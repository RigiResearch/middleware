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
     * Map of previously created resources that can be reused, based on their id.
     */
    private final Map<String, Resource> index;

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
        this.index = new HashMap<>(Data2Hcl.INITIAL_CAPACITY);
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
        // TODO Create issue when VM no longer exists in vmware
        //   (CAM deployment must be removed too)
        this.data.get("getVcenterVm").forEach(this::handleVm);
        return this.spec;
    }

    /**
     * Creates a VM resource.
     * @param node The JSON node that represents the VM resource
     */
    private void handleVm(final JsonNode node) {
        final JsonNode value = node.get("value");
        final String vmid = node.get("vm").asText();
        // The vm
        final Resource resource = HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("resource");
        resource.setType("vsphere_virtual_machine");
        resource.setName(this.reserveName("resource", "vm_%d"));
        // The vm attributes
        final Dictionary attributes = HclFactory.eINSTANCE.createDictionary();
        final Resource name = this.variable(
            attributes,
            String.format("%s_name", resource.getName()),
            "name",
            "string"
        );
        this.values.put(name.getName(), node.get("vm").asText());
        final Resource folder = this.variable(
            attributes,
            String.format("%s_folder", resource.getName()),
            "folder",
            "string"
        );
        this.values.put(
            folder.getName(),
            this.elementGroup("listVcenterVmFilteredByFolder", vmid).get()
        );
        final Resource cpus = this.variable(
            attributes,
            String.format("%s_number_of_vcpu", resource.getName()),
            "num_cpus",
            "integer"
        );
        this.values.put(cpus.getName(), value.get("cpu").get("count").asText());
        final Resource memory = this.variable(
            attributes,
            String.format("%s_memory", resource.getName()),
            "memory",
            "integer"
        );
        this.values.put(memory.getName(), value.get("memory").get("size_MiB").asText());
        // TODO disks
        // TODO resource pool
        // TODO datastore
        // TODO guest_id (changes? from where should I get it?)
        // TODO scsi_type (changes?)
        // The network interfaces
        value.get("nics").forEach(
            nic -> this.handleNic(vmid, resource.getName(), nic, attributes)
        );
        resource.setValue(attributes);
        this.spec.getResources().add(resource);
    }

    /**
     * Creates resources for the given network interface.
     * @param vmid The id of the containing vm
     * @param vmname The reserved name for the vm
     * @param node The JSON node
     * @param attributes The VM attributes to update
     */
    @SuppressWarnings({
        "checkstyle:ParameterNumber",
        "checkstyle:ExecutableStatementCount"
    })
    private void handleNic(final String vmid, final String vmname,
        final JsonNode node, final Dictionary attributes) {
        final JsonNode value = node.get("value");
        final NameValuePair attribute = HclFactory.eINSTANCE.createNameValuePair();
        final Dictionary dictionary = HclFactory.eINSTANCE.createDictionary();
        // TODO other attributes?
        final Resource type = this.variable(
            dictionary,
            String.format("%s_adapter_type", vmname),
            "adapter_type",
            "string"
        );
        this.values.put(type.getName(), value.get("type").asText());
        // Network id: data + variable resources
        final String netname = value.get("backing").get("network_name").asText();
        final Resource netdata;
        if (this.index.containsKey(netname)) {
            netdata = this.index.get(netname);
        } else {
            netdata = HclFactory.eINSTANCE.createResource();
            final Dictionary netvalue = HclFactory.eINSTANCE.createDictionary();
            netdata.setSpecifier("data");
            netdata.setType("vsphere_network");
            netdata.setName(this.reserveName("data", "network_%d"));
            // Network label
            final Resource labelvar = this.variable(
                netvalue,
                String.format("%s_interface_label", netdata.getName()),
                "name",
                "string"
            );
            this.values.put(labelvar.getName(), netname);
            // Datacenter
            final String dataname =
                this.elementGroup("listVcenterVmFilteredByDatacenter", vmid).get();
            final Resource datadata = this.findOrCreateDatacenterData(dataname);
            final NameValuePair dataattr = HclFactory.eINSTANCE.createNameValuePair();
            dataattr.setName("datacenter_id");
            dataattr.setValue(
                this.reference("data", "vsphere_datacenter", datadata.getName(), "id")
            );
            netvalue.getElements().add(dataattr);
            netdata.setValue(netvalue);
            this.spec.getResources().add(netdata);
            this.index.put(netname, netdata);
        }
        final NameValuePair networki = HclFactory.eINSTANCE.createNameValuePair();
        networki.setName("network_id");
        networki.setValue(
            this.reference("data", "vsphere_network", netdata.getName(), "id")
        );
        dictionary.getElements().add(networki);
        attribute.setName("network_interface");
        attribute.setValue(dictionary);
        attributes.getElements().add(attribute);
    }

    /**
     * Finds or create a datacenter's data resource.
     * @param id The datacenter's id
     * @return The data resource
     */
    private Resource findOrCreateDatacenterData(final String id) {
        final Resource datacenter;
        if (this.index.containsKey(id)) {
            datacenter = this.index.get(id);
        } else {
            datacenter = HclFactory.eINSTANCE.createResource();
            datacenter.setSpecifier("data");
            datacenter.setType("vsphere_datacenter");
            datacenter.setName(this.reserveName("data", "datacenter_%d"));
            final Dictionary dictionary = HclFactory.eINSTANCE.createDictionary();
            final Resource datavar = this.variable(
                dictionary,
                String.format("%s_name", datacenter.getName()),
                "name",
                "string"
            );
            // Get the name of the datacenter
            String name = "";
            for (final JsonNode tmp : this.data.get("getVcenterDatacenter")) {
                if (tmp.get("datacenter").asText().equals(id)) {
                    name = tmp.get("name").asText();
                    break;
                }
            }
            this.values.put(datavar.getName(), name);
            datacenter.setValue(dictionary);
            this.spec.getResources().add(datacenter);
            this.index.put(id, datacenter);
        }
        return datacenter;
    }

    /**
     * Creates and adds a resource of type variable.
     * @param attributes The vm attributes
     * @param name The name of the variable, optionally containing %d
     * @param attribute The name of the attribute
     * @param type The type of variable
     * @return A non-null resource object (the variable)
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private Resource variable(final Dictionary attributes,
        final String name, final String attribute, final String type) {
        // The template variable
        final Resource variable = HclFactory.eINSTANCE.createResource();
        variable.setSpecifier("variable");
        if (name.contains("%d")) {
            variable.setName(this.reserveName("variable", name));
        } else {
            variable.setName(name);
        }
        final Dictionary value = HclFactory.eINSTANCE.createDictionary();
        final NameValuePair typep = HclFactory.eINSTANCE.createNameValuePair();
        final Text text = HclFactory.eINSTANCE.createText();
        text.setValue(type);
        typep.setName("type");
        typep.setValue(text);
        value.getElements().add(typep);
        variable.setValue(value);
        this.spec.getResources().add(variable);
        // The attribute
        final NameValuePair pair = HclFactory.eINSTANCE.createNameValuePair();
        pair.setName(attribute);
        pair.setValue(this.reference("var", variable.getName()));
        attributes.getElements().add(pair);
        return variable;
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
    private Optional<String> elementGroup(
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
     * Reserves a resource name based on the number of instances with that same
     * name. This method will reserve the name using a prefix, so that all
     * associated resources of the corresponding resource will share the same
     * prefix. The prefix consists of the characters before {@code %d}.
     * @param specifier The resource specifier.
     * @param name The resource name format (including %d somewhere).
     * @return A non-null reserved name
     */
    private String reserveName(final String specifier, final String name) {
        if (!name.contains("%d")) {
            throw new IllegalArgumentException("Expected an integer specifier (%d)");
        }
        if (!this.instances.containsKey(specifier)) {
            this.instances.put(specifier, new HashMap<>(Data2Hcl.INITIAL_CAPACITY));
        }
        final int number;
        final String prefix = name.substring(0, name.indexOf("%d"));
        if (this.instances.get(specifier).containsKey(prefix)) {
            number = this.instances.get(specifier).get(prefix) + 1;
        } else {
            number = 1;
        }
        this.instances.get(specifier).put(prefix, number);
        return String.format(name, number);
    }

}
