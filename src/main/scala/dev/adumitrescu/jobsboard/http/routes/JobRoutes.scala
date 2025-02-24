package dev.adumitrescu.jobsboard.http.routes

import cats.effect.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.{Monad, MonadThrow}
import cats.implicits.*
import dev.adumitrescu.jobsboard.config.PaginationConfig
import dev.adumitrescu.jobsboard.core.Jobs
import dev.adumitrescu.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import dev.adumitrescu.jobsboard.domain.pagination.Pagination
import dev.adumitrescu.jobsboard.http.responses.FailureResponse
import dev.adumitrescu.jobsboard.http.validation.syntax.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*

import java.util.UUID
import org.typelevel.log4cats.Logger
import dev.adumitrescu.jobsboard.logging.syntax.*
import org.http4s.dsl.io.Root
class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F], paginationConfig: PaginationConfig) extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")
  // POST /jobs?limit=x&offset=y { filters } //TODO add query params and filter
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset)=>
      for {
        filter <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(paginationConfig)(limit, offset))
        resp     <- Ok(jobsList)
      } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.findById(id).flatMap {
      case None      => NotFound(FailureResponse(s"Could not find job with $id"))
      case Some(job) => Ok(job)
    }
  }
  // refined - library to validate
  // checked at compile time - increase compilation time
  // lowers developers experience

  // POST /jobs/create { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "user@adumitrescu.dev",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          id   <- jobs.create("TODO@adumitrescu.dev", jobInfo)
          resp <- Created(id)
        } yield resp
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          maybeNewJob <- jobs.update(id, jobInfo)
          resp <- maybeNewJob match {
            case None      => NotFound(FailureResponse(s"Job with id $id not found"))
            case Some(job) => Ok()
          }
        } yield resp
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      jobs.findById(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot delete job with id $id"))
        case Some(_) =>
          for {
            _    <- jobs.delete(id)
            resp <- Ok()
          } yield resp
      }

  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F], paginationConfig: PaginationConfig) = new JobRoutes[F](jobs, paginationConfig)
}
