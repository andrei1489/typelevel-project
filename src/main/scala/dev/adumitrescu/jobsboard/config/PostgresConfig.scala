package dev.adumitrescu.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class PostgresConfig(nThreads: Int, url: String, user: String, password: String)
    derives ConfigReader
