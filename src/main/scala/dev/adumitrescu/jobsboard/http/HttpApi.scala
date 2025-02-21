package dev.adumitrescu.jobsboard.http

import cats.effect.Concurrent
import cats.{Monad, MonadThrow}
import cats.implicits.*
import dev.adumitrescu.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.server.*

class HttpApi[F[_]: Concurrent] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F].routes

  val endpoints = Router("/api" -> (healthRoutes <+> jobRoutes))
}

object HttpApi {
  def apply[F[_]: Concurrent] = new HttpApi[F]
}
