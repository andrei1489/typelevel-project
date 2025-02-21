package dev.adumitrescu.jobsboard

import cats.*
import cats.implicits.*
import cats.effect.*
import dev.adumitrescu.jobsboard.config.*
import dev.adumitrescu.jobsboard.config.syntax.*
import dev.adumitrescu.jobsboard.http.routes.HealthRoutes
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

/*
  1 - add a plain health endpoint to our app
  2 - add minimal configuration
  3 - basic http server layout
 */

object Application extends IOApp.Simple {

  val configSource = ConfigSource.default.load[EmberConfig]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO,EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HealthRoutes[IO].routes.orNotFound)
        .build
        .use(_ => IO.println("Server ready") *> IO.never)
    }

}
