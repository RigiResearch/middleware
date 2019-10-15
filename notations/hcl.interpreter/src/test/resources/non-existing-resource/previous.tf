data "vsphere_datastore" "vm_1_datastore" {
  name          = "${var.vm_1_disk_datastore}"
  datacenter_id = "${data.vsphere_datacenter.vm_1_datacenter.id}"
}

data "vsphere_resource_pool" "vm_1_resource_pool" {
  name          = "${var.vm_1_resource_pool}"
  datacenter_id = "${data.vsphere_datacenter.vm_1_datacenter.id}"
}
