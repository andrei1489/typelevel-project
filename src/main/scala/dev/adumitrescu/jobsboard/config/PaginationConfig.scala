package dev.adumitrescu.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PaginationConfig (defaultPageSize: Int = 20) derives ConfigReader
