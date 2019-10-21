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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformation to create an HCL model from collected data from vSphere.
 * TODO Improve this implementation. Separate the Json node from the creation of
 *  resources (associated with PMD.TooManyMethods).
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.AvoidDuplicateLiterals",
    "checkstyle:ParameterNumber",
    "checkstyle:ExecutableStatementCount"
})
public final class Data2Hcl {

    /**
     * Initial capacity for lists and maps.
     */
    private static final int INITIAL_CAPACITY = 10;

    /**
     * The number of bytes in a gigabyte.
     */
    private static final double BYTES_IN_A_GB = StrictMath.pow(2.0, 30.0);

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
        // TODO Create issue when VM no longer exists in vmware
        //   (CAM deployment must be removed too)
        // Add resources associated with the provider
        // - var allow_unverified_ssl
        final Resource unverified = HclFactory.eINSTANCE.createResource();
        final Dictionary attrs = HclFactory.eINSTANCE.createDictionary();
        unverified.setSpecifier("variable");
        unverified.setName("allow_unverified_ssl");
        unverified.setValue(attrs);
        final NameValuePair description = HclFactory.eINSTANCE.createNameValuePair();
        description.setName("description");
        final Text desctext = HclFactory.eINSTANCE.createText();
        desctext.setValue("Communication with vsphere server with self signed certificate");
        description.setValue(desctext);
        final NameValuePair value = HclFactory.eINSTANCE.createNameValuePair();
        value.setName("default");
        final Text valuetext = HclFactory.eINSTANCE.createText();
        valuetext.setValue("true");
        value.setValue(valuetext);
        attrs.getElements().add(description);
        attrs.getElements().add(value);
        this.spec.getResources().add(unverified);
        // - provider vsphere
        final Resource vsphere = HclFactory.eINSTANCE.createResource();
        final Dictionary vattrs = HclFactory.eINSTANCE.createDictionary();
        vsphere.setSpecifier("provider");
        vsphere.setName("vsphere");
        vsphere.setValue(vattrs);
        final NameValuePair ssl = HclFactory.eINSTANCE.createNameValuePair();
        ssl.setName("allow_unverified_ssl");
        ssl.setValue(this.reference("var", "allow_unverified_ssl"));
        final NameValuePair version = HclFactory.eINSTANCE.createNameValuePair();
        version.setName("version");
        final Text versiontext = HclFactory.eINSTANCE.createText();
        versiontext.setValue("~> 1.3");
        version.setValue(versiontext);
        vattrs.getElements().add(version);
        vattrs.getElements().add(ssl);
        this.spec.getResources().add(vsphere);
        // - provider camc
        final Resource camc = HclFactory.eINSTANCE.createResource();
        final Dictionary cattrs = HclFactory.eINSTANCE.createDictionary();
        camc.setSpecifier("provider");
        camc.setName("camc");
        camc.setValue(cattrs);
        final NameValuePair camversion = HclFactory.eINSTANCE.createNameValuePair();
        camversion.setName("version");
        final Text versionattr = HclFactory.eINSTANCE.createText();
        versionattr.setValue("~> 0.2");
        camversion.setValue(versionattr);
        cattrs.getElements().add(camversion);
        this.spec.getResources().add(camc);
        this.data.get("getVcenterVm").forEach(this::handleVm);
        return this.spec;
    }

    /**
     * Creates a VM resource.
     * @param node The JSON node that represents the VM resource
     */
    private void handleVm(final JsonNode node) {
        final String vmid = node.get("vm").asText();
        // 1. The vm
        final Resource resource = HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("resource");
        resource.setType("vsphere_virtual_machine");
        resource.setName(this.reserveName("resource", "vm_%d"));
        // 2. The vm attributes
        final Dictionary attributes = HclFactory.eINSTANCE.createDictionary();
        // - Name
        final Resource name = this.variable(
            attributes,
            String.format("%s_name", resource.getName()),
            "name",
            "string"
        );
        this.values.put(name.getName(), node.get("name").asText());
        // - Folder
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
        // - CPUs
        final Resource cpus = this.variable(
            attributes,
            String.format("%s_number_of_cpus", resource.getName()),
            "num_cpus",
            "string"
        );
        this.values.put(cpus.getName(), node.get("cpu").get("count").asText());
        // - Cores per Socket
        final Resource cores = this.variable(
            attributes,
            String.format("%s_num_cores_per_socket", resource.getName()),
            "num_cores_per_socket",
            "string"
        );
        this.values.put(cores.getName(), node.get("cpu").get("cores_per_socket").asText());
        // - Memory
        final Resource memory = this.variable(
            attributes,
            String.format("%s_memory", resource.getName()),
            "memory",
            "string"
        );
        this.values.put(memory.getName(), node.get("memory").get("size_MiB").asText());
        // - Resource pool
        final String dataname =
            this.elementGroup("listVcenterVmFilteredByResourcePool", vmid).get();
        final String datacenter =
            this.elementGroup("listVcenterVmFilteredByDatacenter", vmid).get();
        final Resource respool = this.findOrCreateResourcePoolData(dataname, datacenter);
        final NameValuePair respooli = HclFactory.eINSTANCE.createNameValuePair();
        respooli.setName("resource_pool_id");
        respooli.setValue(
            this.reference(
                respool.getSpecifier(),
                respool.getType(),
                respool.getName(),
                "id"
            )
        );
        attributes.getElements().add(respooli);
        // - Guest OS
        final Resource guest = this.variable(
            attributes,
            String.format("%s_guest_os_id", resource.getName()),
            "guest_id",
            "string"
        );
        this.values.put(guest.getName(), node.get("guest_OS").asText());
        // - SCSI type
        final Resource scsi = this.variable(
            attributes,
            String.format("%s_scsi_type", resource.getName()),
            "scsi_type",
            "string"
        );
        this.values.put(scsi.getName(), this.scsiType(node.get("scsi_adapters")));
        // - Disks
        node.get("disks").forEach(
            disk -> this.handleDisk(resource.getName(), datacenter, disk, attributes)
        );
        // - Network interfaces
        node.get("nics").forEach(
            nic -> this.handleNic(resource.getName(), datacenter, nic, attributes)
        );
        // TODO cdrom
        //  terraform.io/docs/providers/vsphere/r/virtual_machine.html#cdrom-options
        resource.setValue(attributes);
        this.spec.getResources().add(resource);
    }

    /**
     * Chooses an appropriate SCSI type for a VM based on its SCSI adapters.
     * See more at
     * https://www.terraform.io/docs/providers/vsphere/d/virtual_machine.html#scsi_type
     * @param adapters The adapters node
     * @return A non-null, non-empty string
     */
    private String scsiType(final JsonNode adapters) {
        final String type;
        final Set<String> types = new HashSet<>(Data2Hcl.INITIAL_CAPACITY);
        for (final JsonNode adapter : adapters) {
            types.add(
                adapter.get("value")
                    .get("type")
                    .asText()
                    .toLowerCase(Locale.getDefault())
            );
        }
        if (types.size() > 1) {
            type = "mixed";
        } else if (types.isEmpty()) {
            // TODO Is this even possible?
            type = "mixed";
        } else {
            type = types.iterator().next();
        }
        return type;
    }

    /**
     * Creates resources for the given disk.
     * @param vmname The VM id
     * @param datacenter The datacenter id
     * @param node The JSON node
     * @param attributes The VM attributes
     */
    private void handleDisk(final String vmname, final String datacenter,
        final JsonNode node, final Dictionary attributes) {
        final JsonNode value = node.get("value");
        final String diskn =
            this.reserveName("var", String.format("%s_disk_%%d", vmname));
        final NameValuePair attribute = HclFactory.eINSTANCE.createNameValuePair();
        final Dictionary dictionary = HclFactory.eINSTANCE.createDictionary();
        attribute.setName("disk");
        attribute.setValue(dictionary);
        // - Size
        final Resource size = this.variable(
            dictionary,
            String.format("%s_size", diskn),
            "size",
            "string"
        );
        final long capacity =
            Math.round(value.get("capacity").asDouble() / Data2Hcl.BYTES_IN_A_GB);
        this.values.put(size.getName(), String.valueOf(capacity));
        // - Unit number
        final Resource unit = this.variable(
            dictionary,
            String.format("%s_unit_number", diskn),
            "unit_number",
            "string"
        );
        this.values.put(unit.getName(), value.get("scsi").get("unit").asText());
        // - Label and Datastore
        final Pattern pattern = Pattern.compile("\\[([\\w\\-]+)\\]\\s(.+)");
        final Matcher matcher =
            pattern.matcher(value.get("backing").get("vmdk_file").asText());
        if (matcher.find()) {
            // - Label
            // TODO this may be wrong (not sure if this should be "path" instead)
            final Resource label = this.variable(
                dictionary,
                String.format("%s_label", diskn),
                "label",
                "string"
            );
            this.values.put(label.getName(), matcher.group(2));
            // - Datastore
            final Resource dsdata =
                this.findOrCreateDatastoreData(matcher.group(1), datacenter);
            final NameValuePair datastorei =
                HclFactory.eINSTANCE.createNameValuePair();
            datastorei.setName("datastore_id");
            datastorei.setValue(
                this.reference(
                    dsdata.getSpecifier(),
                    dsdata.getType(),
                    dsdata.getName(),
                    "id"
                )
            );
            dictionary.getElements().add(datastorei);
        }
        attributes.getElements().add(attribute);
    }

    /**
     * Creates resources for the given network interface.
     * @param vmname The reserved name for the vm
     * @param datacenter The datacenter's id
     * @param node The JSON node
     * @param attributes The VM attributes to update
     */
    private void handleNic(final String vmname, final String datacenter,
        final JsonNode node, final Dictionary attributes) {
        final JsonNode value = node.get("value");
        final NameValuePair attribute = HclFactory.eINSTANCE.createNameValuePair();
        final Dictionary dictionary = HclFactory.eINSTANCE.createDictionary();
        // TODO other attributes?
        final Resource type = this.variable(
            dictionary,
            String.format("%s_adapter_%%d_type", vmname),
            "adapter_type",
            "string"
        );
        this.values.put(
            type.getName(),
            value.get("type").asText()
                .toLowerCase(Locale.getDefault())
        );
        // Network id: data + variable resources
        final String netname = value.get("backing").get("network_name").asText();
        final Resource netdata;
        final String prefix = "network-";
        if (this.index.containsKey(prefix + netname)) {
            netdata = this.index.get(prefix + netname);
        } else {
            netdata = HclFactory.eINSTANCE.createResource();
            final Dictionary netvalue = HclFactory.eINSTANCE.createDictionary();
            netdata.setSpecifier("data");
            netdata.setType("vsphere_network");
            netdata.setName(this.reserveName("data", "network_%d"));
            // Network label
            final Resource labelvar = this.variable(
                netvalue,
                String.format("%s_interface_%%d_label", netdata.getName()),
                "name",
                "string"
            );
            this.values.put(labelvar.getName(), netname);
            // Datacenter
            final Resource datadata = this.findOrCreateDatacenterData(datacenter);
            final NameValuePair dataattr = HclFactory.eINSTANCE.createNameValuePair();
            dataattr.setName("datacenter_id");
            dataattr.setValue(
                this.reference(
                    datadata.getSpecifier(),
                    datadata.getType(),
                    datadata.getName(),
                    "id")
            );
            netvalue.getElements().add(dataattr);
            netdata.setValue(netvalue);
            this.spec.getResources().add(netdata);
            this.index.put(prefix + netname, netdata);
        }
        final NameValuePair networki = HclFactory.eINSTANCE.createNameValuePair();
        networki.setName("network_id");
        networki.setValue(
            this.reference(
                netdata.getSpecifier(),
                netdata.getType(),
                netdata.getName(),
                "id"
            )
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
        final String prefix = "datacenter-";
        if (this.index.containsKey(prefix + id)) {
            datacenter = this.index.get(prefix + id);
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
            this.index.put(prefix + id, datacenter);
        }
        return datacenter;
    }

    /**
     * Finds or create a datastore's data resource.
     * @param id The datastore's id
     * @param datacenter The associated datastore's id
     * @return The data resource
     */
    private Resource findOrCreateDatastoreData(final String id,
        final String datacenter) {
        final Resource datastore;
        final String prefix = "datastore-";
        if (this.index.containsKey(prefix + id)) {
            datastore = this.index.get(prefix + id);
        } else {
            datastore = HclFactory.eINSTANCE.createResource();
            datastore.setSpecifier("data");
            datastore.setType("vsphere_datastore");
            datastore.setName(this.reserveName("datastore", "datastore_%d"));
            final Dictionary attributes = HclFactory.eINSTANCE.createDictionary();
            datastore.setValue(attributes);
            // - name
            final Resource namevar = this.variable(
                attributes,
                String.format("%s_name", datastore.getName()),
                "name",
                "string"
            );
            this.values.put(namevar.getName(), id);
            // - datacenter_id
            final Resource dcdata = this.findOrCreateDatacenterData(datacenter);
            final NameValuePair datacenteri = HclFactory.eINSTANCE.createNameValuePair();
            datacenteri.setName("datacenter_id");
            datacenteri.setValue(
                this.reference(
                    dcdata.getSpecifier(),
                    dcdata.getType(),
                    dcdata.getName(),
                    "id"
                )
            );
            attributes.getElements().add(datacenteri);
            this.spec.getResources().add(datastore);
            this.index.put(prefix + id, datastore);
        }
        return datastore;
    }

    /**
     * Finds or create a resource pool's data resource.
     * @param id The resource pool's id
     * @param datacenter The corresponding datacenter's id
     * @return The data resource
     */
    private Resource findOrCreateResourcePoolData(final String id,
        final String datacenter) {
        final Resource respool;
        final String prefix = "resource-pool-";
        if (this.index.containsKey(prefix + id)) {
            respool = this.index.get(prefix + id);
        } else {
            respool = HclFactory.eINSTANCE.createResource();
            respool.setSpecifier("data");
            respool.setType("vsphere_resource_pool");
            respool.setName(this.reserveName("data", "resource_pool_%d"));
            final Dictionary attributes = HclFactory.eINSTANCE.createDictionary();
            final Resource datavar = this.variable(
                attributes,
                String.format("%s_name", respool.getName()),
                "name",
                "string"
            );
            // - name
            // Get the name of the resource pool
            String name = "";
            for (final JsonNode tmp : this.data.get("getVcenterResourcePool")) {
                if (tmp.get("resource_pool").asText().equals(id)) {
                    name = tmp.get("name").asText();
                    break;
                }
            }
            this.values.put(datavar.getName(), name);
            // - datacenter_id
            final Resource dcdata = this.findOrCreateDatacenterData(datacenter);
            final NameValuePair datacenteri = HclFactory.eINSTANCE.createNameValuePair();
            datacenteri.setName("datacenter_id");
            datacenteri.setValue(
                this.reference(
                    dcdata.getSpecifier(),
                    dcdata.getType(),
                    dcdata.getName(),
                    "id"
                )
            );
            attributes.getElements().add(datacenteri);
            respool.setValue(attributes);
            this.spec.getResources().add(respool);
            this.index.put(prefix + id, respool);
        }
        return respool;
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
