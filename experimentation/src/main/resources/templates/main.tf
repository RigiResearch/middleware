variable "os_auth_url" {}

variable "os_password" {}

variable "cluster_nodes" {}

variable "vm_memory" {}

variable "vm_cpus" {}

module "rke" {
  source  = "remche/rke/openstack"
  image_name          = "Ubuntu-18.04-Bionic-x64-2020-12"
  public_net_name     = "Public-Network"
  master_flavor_name  = "p2-3gb"
  worker_flavor_name  = "p${var.vm_cpus}-${var.vm_memory}"
  worker_count        = var.cluster_nodes
  os_auth_url         = var.os_auth_url
  os_password         = var.os_password
}
