package dev.adumitrescu.jobsboard.modules

import cats.effect.{Concurrent, Resource}
import cats.{Monad, MonadThrow}
import cats.implicits.*
import dev.adumitrescu.jobsboard.config.PaginationConfig
import dev.adumitrescu.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private (
    core: Core[F],
    paginationConfig: PaginationConfig
) {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F](core.jobs, paginationConfig).routes

  val endpoints = Router("/api" -> (healthRoutes <+> jobRoutes))
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger](
      core: Core[F],
      paginationConfig: PaginationConfig
  ): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core, paginationConfig))
}
