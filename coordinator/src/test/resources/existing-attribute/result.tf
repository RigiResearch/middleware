data "vsphere_datastore" "vm_1_datastore" {
  name          = "${var.vm_1_root_disk_datastore}"
  datacenter_id = "${data.vsphere_datacenter.vm_1_datacenter.id}"
}
