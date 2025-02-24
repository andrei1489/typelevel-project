package dev.adumitrescu.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dev.adumitrescu.jobsboard.config.PaginationConfig
import dev.adumitrescu.jobsboard.domain.job.JobFilter
import dev.adumitrescu.jobsboard.domain.pagination.Pagination
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import dev.adumitrescu.jobsboard.fixtures.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobsSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with JobFixture {
  val initScript: String = "sql/jobs.sql"

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.findById(NotFoundJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }
    "should retrieve a job by id" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.findById(AwesomeJobUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))
      }
    }
    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }
    "should create a new job" in {
      transactor.use { xa =>
        val program = for {
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("andrei@adumitrescu.dev", RockTheJvmNewJob)
          maybeJob <- jobs.findById(jobId)
        } yield maybeJob

        program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
      }
    }
    "should return an updated job if it exists" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)

        } yield maybeUpdatedJob

        program.asserting(_ shouldBe Some(UpdatedAwesomeJob))
      }
    }
    "should return None when trying to update a  job if it does not exists" in {
      transactor.use { xa =>
        val program = for {
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)

        } yield maybeUpdatedJob

        program.asserting(_ shouldBe None)
      }
    }

    "should delete an existing job" in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          countOfJobs <- sql"select count(*) from jobs where id = $AwesomeJobUuid"
            .query[Int]
            .unique
            .transact(xa)

        } yield (numberOfDeletedJobs, countOfJobs)

        program.asserting { case (numberOfDeletedJobs, countOfJobs) =>
          numberOfDeletedJobs shouldBe 1
          countOfJobs shouldBe 0
        }
      }
    }
    "should return 0 updated rows if the job id to delete is not found " in {
      transactor.use { xa =>
        val program = for {
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
        } yield numberOfDeletedJobs

        program.asserting(_ shouldBe 0)
      }
    }
    "should filter remote jobs " in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(
            JobFilter(remote = true),
            Pagination(0,10)
          )
        } yield filteredJobs

        program.asserting(_ shouldBe List())
      }
    }
    "should filter jobs by tags " in {
      transactor.use { xa =>
        val program = for {
          jobs <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(
            JobFilter(tags = List("scala", "cats", "zio")),
            Pagination(10, 0)
          )
        } yield filteredJobs

        program.asserting(_ shouldBe List(AwesomeJob))
      }
    }
  }
}
