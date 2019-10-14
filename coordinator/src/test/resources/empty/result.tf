##############################################################
# Keys (public/private) and optional user key (public)
##############################################################
variable "allow_unverified_ssl" {
  default     = "true"
  description = "Communication with vsphere server with self signed certificate"
}
