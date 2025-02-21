package dev.adumitrescu.jobsboard.http.routes

import cats.effect.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.{Monad, MonadThrow}
import cats.implicits.*
import dev.adumitrescu.jobsboard.domain.job.{Job, JobInfo}
import dev.adumitrescu.jobsboard.http.responses.FailureResponse
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*

import java.util.UUID
import scala.collection.mutable
import org.typelevel.log4cats.Logger

class JobRoutes[F[_]: Concurrent: Logger] private extends Http4sDsl[F] {
  // database
  private val database = mutable.Map[UUID, Job]()
  // POST /jobs?offset=x&limit=y { filters } //TODO add query params and filter
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root => Ok(database.values)
  }

  //GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      database.get(id) match {
        case None      => NotFound(FailureResponse(s"Could not find job with $id"))
        case Some(job) => Ok(job)
      }
  }

  //POST /jobs/create { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "user@adumitrescu.dev",
      jobInfo = jobInfo,
      active = true
    ).pure[F]
  import dev.adumitrescu.jobsboard.logging.syntax.*
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        job <- createJob(jobInfo)
        _ <- database.put(job.id, job).pure[F]
        resp <- Created(job.id)
      } yield resp
  }

  //PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case None => NotFound(FailureResponse(s"Job with id $id not found"))
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp <- Ok()
          } yield resp
      }
  }

  //DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case None => NotFound(FailureResponse(s"Cannot delete job with id $id"))
        case Some(_) =>
          for {
            _ <- database.remove(id).pure[F]
            resp <- Ok()
          } yield resp
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger] = new JobRoutes[F]
}
