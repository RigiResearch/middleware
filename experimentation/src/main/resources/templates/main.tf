variable "os_auth_url" {}

variable "os_password" {}

variable "cluster_nodes" {}

variable "vm_memory" {}

variable "vm_cpus" {}

variable "flavor" {}

module "control_plane" {
  source           = "remche/rke2/openstack"
  version          = "0.3.0"
  use_ssh_agent    = false
  cluster_name     = "cluster-${var.cluster_nodes}--${var.vm_cpus}-${var.vm_memory}"
  write_kubeconfig = true
  image_name       = "Ubuntu-20.04.3-Focal-x64-2021-12"
  flavor_name      = "p8-12gb"
  public_net_name  = "Public-Network"
  rke2_config_file = "rke2_config.yaml"
  manifests_path   = "./manifests"
}

module "worker_node" {
  source      = "remche/rke2/openstack//modules/agent"
  version     = "0.3.0"
  image_name  = "Ubuntu-20.04.3-Focal-x64-2021-12"
  nodes_count = var.cluster_nodes
  name_prefix = "worker"
  flavor_name = "${var.flavor}"
  node_config = module.control_plane.node_config
}

output "controlplane_floating_ip" {
  value     = module.control_plane.floating_ip
  sensitive = true
}
