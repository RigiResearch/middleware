variable "datacenter_1_name" {
  type = "string"
}

variable "datastore_1_name" {
  type = "string"
}

variable "network_1_interface_1_label" {
  type = "string"
}

variable "resource_pool_1_name" {
  type = "string"
}

variable "vm_1_adapter_1_type" {
  type = "string"
}

variable "vm_1_disk_1_label" {
  type = "string"
}

variable "vm_1_disk_1_size" {
  type = "string"
}

variable "vm_1_disk_1_unit_number" {
  type = "string"
}

variable "vm_1_disk_2_label" {
  type = "string"
}

variable "vm_1_disk_2_size" {
  type = "string"
}

variable "vm_1_disk_2_unit_number" {
  type = "string"
}

variable "vm_1_folder" {
  type = "string"
}

variable "vm_1_guest_os_id" {
  type = "string"
}

variable "vm_1_memory" {
  type = "string"
}

variable "vm_1_name" {
  type = "string"
}

variable "vm_1_num_cores_per_socket" {
  type = "string"
}

variable "vm_1_number_of_cpus" {
  type = "string"
}

variable "vm_1_scsi_type" {
  type = "string"
}

data "vsphere_datacenter" "datacenter_1" {
  name = "${var.datacenter_1_name}"
}

data "vsphere_datastore" "datastore_1" {
  datacenter_id = "${data.vsphere_datacenter.datacenter_1.id}"
  name          = "${var.datastore_1_name}"
}

data "vsphere_network" "network_1" {
  datacenter_id = "${data.vsphere_datacenter.datacenter_1.id}"
  name          = "${var.network_1_interface_1_label}"
}

data "vsphere_resource_pool" "resource_pool_1" {
  datacenter_id = "${data.vsphere_datacenter.datacenter_1.id}"
  name          = "${var.resource_pool_1_name}"
}

resource "vsphere_virtual_machine" "vm_1" {
  folder               = "${var.vm_1_folder}"
  guest_id             = "${var.vm_1_guest_os_id}"
  memory               = "${var.vm_1_memory}"
  name                 = "${var.vm_1_name}"
  num_cores_per_socket = "${var.vm_1_num_cores_per_socket}"
  num_cpus             = "${var.vm_1_number_of_cpus}"
  resource_pool_id     = "${data.vsphere_resource_pool.resource_pool_1.id}"
  scsi_type            = "${var.vm_1_scsi_type}"

  disk {
    datastore_id = "${data.vsphere_datastore.datastore_1.id}"
    label        = "${var.vm_1_disk_1_label}"
    size         = "${var.vm_1_disk_1_size}"
    unit_number  = "${var.vm_1_disk_1_unit_number}"
  }

  disk {
    datastore_id = "${data.vsphere_datastore.datastore_1.id}"
    label        = "${var.vm_1_disk_2_label}"
    size         = "${var.vm_1_disk_2_size}"
    unit_number  = "${var.vm_1_disk_2_unit_number}"
  }

  network_interface {
    adapter_type = "${var.vm_1_adapter_1_type}"
    network_id   = "${data.vsphere_network.network_1.id}"
  }
}
