package dev.adumitrescu.jobsboard.playground
import cats.effect.*
import dev.adumitrescu.jobsboard.core.LiveJobs
import dev.adumitrescu.jobsboard.domain.job.*
import doobie.*
import doobie.implicits.*
import doobie.hikari.HikariTransactor

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {

  val postgresResource: Resource[IO,HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Engineer",
    description = "Best Job ever",
    externalUrl = "rockthejvm.com",
    remote= true,
    location = "Bucharest"
  )
  override def run: IO[Unit] = postgresResource.use {
    xa => for {
      jobs <- LiveJobs[IO](xa)
      _ <- IO(println("Ready.Next...")) *> IO(StdIn.readLine())
      id <- jobs.create("andrei@adumitrescu.dev", jobInfo)
      _ <- IO(println("Next...")) *> IO(StdIn.readLine())
      list <- jobs.all()
      _ <- IO(println(s"All jobs: $list . Next...")) *> IO(StdIn.readLine())
      _ <- jobs.update(id, jobInfo.copy(title = "Software Rockstar"))
      job <- jobs.findById(id)
      _ <- IO(println(s"New Job: $job . Next...")) *> IO(StdIn.readLine())
      _ <- jobs.delete(id)
      listAfter <- jobs.all()
      _ <- IO(println(s"Deleted Job. List now: $listAfter")) *> IO(StdIn.readLine())
    } yield ()
  }

}
