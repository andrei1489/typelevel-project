package dev.adumitrescu.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class AppConfig(
    postgresConfig: PostgresConfig,
    emberConfig: EmberConfig,
    paginationConfig: PaginationConfig
) derives ConfigReader
