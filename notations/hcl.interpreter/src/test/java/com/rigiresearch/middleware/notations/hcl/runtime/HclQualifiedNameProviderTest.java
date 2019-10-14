package com.rigiresearch.middleware.notations.hcl.runtime;

import com.rigiresearch.middleware.metamodels.hcl.HclFactory;
import com.rigiresearch.middleware.metamodels.hcl.Resource;
import org.eclipse.xtext.naming.QualifiedName;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link HclQualifiedNameProvider}.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
class HclQualifiedNameProviderTest {

    /**
     * The delimiter to use.
     */
    private static final String DELIMITER = ".";

    /**
     * The provider.
     */
    private final HclQualifiedNameProvider provider;

    /**
     * Default constructor.
     */
    HclQualifiedNameProviderTest() {
        this.provider = new HclQualifiedNameProvider();
    }

    @Test
    void testVariableQualifiedName() {
        final Resource variable = HclFactory.eINSTANCE.createResource();
        variable.setSpecifier("variable");
        variable.setName("allow_unverified_ssl");
        final QualifiedName name = this.provider.getFullyQualifiedName(variable);
        Assert.assertEquals(
            "Unexpected variable qualified name",
            "variable.allow_unverified_ssl",
            name.toString(HclQualifiedNameProviderTest.DELIMITER)
        );
    }

    @Test
    void testProviderQualifiedName() {
        final Resource prov = HclFactory.eINSTANCE.createResource();
        prov.setSpecifier("provider");
        prov.setName("vsphere");
        final QualifiedName name = this.provider.getFullyQualifiedName(prov);
        Assert.assertEquals(
            "Unexpected provider qualified name",
            "provider.vsphere",
            name.toString(HclQualifiedNameProviderTest.DELIMITER)
        );
    }

    @Test
    void testDataQualifiedName() {
        final Resource data = HclFactory.eINSTANCE.createResource();
        data.setSpecifier("data");
        data.setType("vsphere_datacenter");
        data.setName("vm_1_datacenter");
        final QualifiedName name = this.provider.getFullyQualifiedName(data);
        Assert.assertEquals(
            "Unexpected data qualified name",
            "data.vsphere_datacenter.vm_1_datacenter",
            name.toString(HclQualifiedNameProviderTest.DELIMITER)
        );
    }

    @Test
    void testResourceQualifiedName() {
        final Resource resource = HclFactory.eINSTANCE.createResource();
        resource.setSpecifier("resource");
        resource.setType("vsphere_virtual_machine");
        resource.setName("vm_1");
        final QualifiedName name = this.provider.getFullyQualifiedName(resource);
        Assert.assertEquals(
            "Unexpected resource qualified name",
            "resource.vsphere_virtual_machine.vm_1",
            name.toString(HclQualifiedNameProviderTest.DELIMITER)
        );
    }

}
