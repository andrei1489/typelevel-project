package dev.adumitrescu.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dev.adumitrescu.jobsboard.core.Jobs
import dev.adumitrescu.jobsboard.domain.job.{Job, JobInfo}
import dev.adumitrescu.jobsboard.fixtures.JobFixture
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {

  // prep
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] = IO.pure(List(AwesomeJob))

    override def findById(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(AwesomeJob))
      else IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid) IO.pure(Some(UpdatedAwesomeJob))
      else IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid) IO.pure(1)
      else IO.pure(0)
  }
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val jobsRoutes: HttpRoutes[IO]  = JobRoutes[IO](jobs).routes

  "JobRoutes" - {
    "should return a job with a given id" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        // get http response
        retrieved <- response.as[Job]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }
    "should return all jobs" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
        )
        // get http response
        retrieved <- response.as[List[Job]]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }
    "should create a new job" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        // get http response
        retrieved <- response.as[UUID]

      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }
    "should update a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOK <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f400))")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )

      } yield {
        responseOK.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound

      }
    }
    "should delete a job that only exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOK <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f400))")
        )

      } yield {
        responseOK.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

}
