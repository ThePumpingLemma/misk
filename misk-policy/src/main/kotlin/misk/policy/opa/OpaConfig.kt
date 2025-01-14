package misk.policy.opa

import misk.config.Config

data class OpaConfig(
  val baseUrl: String,
  val unixSocket: String?,
  val provenance: Boolean = false
) : Config
