package dev.adumitrescu.jobsboard

import cats.*
import cats.implicits.*
import cats.effect.*
import dev.adumitrescu.jobsboard.config.*
import dev.adumitrescu.jobsboard.config.syntax.*
import dev.adumitrescu.jobsboard.modules.*
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */

object Application extends IOApp.Simple {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(postgresConfig, emberConfig, paginationConfig) =>
        val appResource = for {
          xa      <- Database.makePostgresResource[IO](postgresConfig)
          core    <- Core[IO](xa)
          httpApi <- HttpApi[IO](core, paginationConfig)
          server <- EmberServerBuilder
            .default[IO]
            .withHost(emberConfig.host)
            .withPort(emberConfig.port)
            .withHttpApp(httpApi.endpoints.orNotFound)
            .build
        } yield server
        appResource.use(_ => IO.println("Server Ready") *> IO.never)
    }

}
