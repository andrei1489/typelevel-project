package dev.adumitrescu.jobsboard.modules

import cats.effect.*
import dev.adumitrescu.jobsboard.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {
  def makePostgresResource[F[_] : Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url,
        config.user,
        config.password,
        ec
      )
    } yield xa

}
