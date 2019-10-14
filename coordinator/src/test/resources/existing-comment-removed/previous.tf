data "vsphere_datastore" "vm_1_datastore" {
  # my existing attribute comment
  name          = "${var.vm_1_disk_datastore}"
  datacenter_id = "${data.vsphere_datacenter.vm_1_datacenter.id}"
}
