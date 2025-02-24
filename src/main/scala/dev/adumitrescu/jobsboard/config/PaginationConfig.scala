package dev.adumitrescu.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PaginationConfig (defaultPageSize: Int) derives ConfigReader
