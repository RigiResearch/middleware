variable "allow_unverified_ssl" {
  default     = "true"
  description = "Communication with vsphere server with self signed certificate"
  type        = "string"
}

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

variable "vm_1_dns_servers" {
  type = "list"
}

variable "vm_1_dns_suffixes" {
  type = "list"
}

variable "vm_1_domain" {
  type = "string"
}

variable "vm_1_disk_1_unit_number" {
  type = "string"
}

variable "vm_1_folder" {
  type = "string"
}

variable "vm_1_image" {
  type = "string"
}

variable "vm_1_ipv4_address" {
  type = "string"
}

variable "vm_1_ipv4_gateway" {
  type = "string"
}

variable "vm_1_ipv4_prefix_length" {
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

provider "camc" {
  version = "~> 0.2"
}

provider "vsphere" {
  allow_unverified_ssl = "${var.allow_unverified_ssl}"
  version              = "~> 1.3"
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

data "vsphere_virtual_machine" "vm_1_template" {
  name          = "${var.vm_1_image}"
  datacenter_id = "${data.vsphere_datacenter.datacenter_1.id}"
}

resource "vsphere_virtual_machine" "vm_1" {
  datastore_id         = "${data.vsphere_datastore.datastore_1.id}"
  folder               = "${var.vm_1_folder}"
  guest_id             = "${var.vm_1_guest_os_id}"
  memory               = "${var.vm_1_memory}"
  name                 = "${var.vm_1_name}"
  num_cores_per_socket = "${var.vm_1_num_cores_per_socket}"
  num_cpus             = "${var.vm_1_number_of_cpus}"
  resource_pool_id     = "${data.vsphere_resource_pool.resource_pool_1.id}"
  scsi_type            = "${var.vm_1_scsi_type}"

  clone {
    template_uuid = "${data.vsphere_virtual_machine.vm_1_template.id}"

    customize {
      dns_server_list = "${var.vm_1_dns_servers}"
      dns_suffix_list = "${var.vm_1_dns_suffixes}"
      ipv4_gateway    = "${var.vm_1_ipv4_gateway}"

      linux_options {
        domain    = "${var.vm_1_domain}"
        host_name = "${var.vm_1_name}"
      }

      network_interface {
        ipv4_address = "${var.vm_1_ipv4_address}"
        ipv4_netmask = "${var.vm_1_ipv4_prefix_length}"
      }
    }
  }

  disk {
    label       = "${var.vm_1_disk_1_label}"
    size        = "${var.vm_1_disk_1_size}"
    unit_number = "${var.vm_1_disk_1_unit_number}"
  }

  network_interface {
    adapter_type = "${var.vm_1_adapter_1_type}"
    network_id   = "${data.vsphere_network.network_1.id}"
  }
}
