# A resource comment
variable "allow_unverified_ssl" {
  description = "Communication with vsphere server with self signed certificate"
  # An attribute comment
  default     = "true"
  # Another comment
  same        = 1
}

# Another resource
variable "name" {
  attr = "value"
}
