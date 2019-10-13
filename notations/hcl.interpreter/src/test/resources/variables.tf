variable "allow_unverified_ssl" {
  description = "Communication with vsphere server with self signed certificate"
  default     = "true"
}

variable "vm_1_folder" {
  description = "Target vSphere folder for virtual machine"
}

variable "vm_1_datacenter" {
  description = "Target vSphere datacenter for virtual machine creation"
}
