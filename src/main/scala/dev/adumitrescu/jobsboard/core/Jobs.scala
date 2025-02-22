package dev.adumitrescu.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import dev.adumitrescu.jobsboard.domain.job.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID

trait Jobs[F[_]] {
  // "algebra"
  // CRUD operations
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def findById(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow] private (xa: Transactor[F])
    extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
         INSERT INTO jobs (
          date,
          ownerEmail,
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHigh,
          currency,
          country,
          tags,
          image,
          seniority,
          other,
          active
         ) values (
         ${System.currentTimeMillis()},
         ${ownerEmail},
         ${jobInfo.company},
         ${jobInfo.title},
         ${jobInfo.description},
         ${jobInfo.externalUrl},
         ${jobInfo.remote},
         ${jobInfo.location},
         ${jobInfo.salaryLo},
         ${jobInfo.salaryHigh},
         ${jobInfo.currency},
         ${jobInfo.country},
         ${jobInfo.tags},
         ${jobInfo.image},
         ${jobInfo.seniority},
         ${jobInfo.other},
         false
         )
       """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)
  override def all(): F[List[Job]] =
    sql"""
         SELECT
         id,
         date,
         ownerEmail,
         company,
         title,
         description,
         externalUrl,
         remote,
         location,
         salaryLo,
         salaryHigh,
         currency,
         country,
         tags,
         image,
         seniority,
         other,
         active
         FROM jobs
       """
      .query[Job]
      .to[List]
      .transact(xa)
  override def findById(id: UUID): F[Option[Job]] =
    sql"""
              SELECT
              id,
              date,
              ownerEmail,
              company,
              title,
              description,
              externalUrl,
              remote,
              location,
              salaryLo,
              salaryHigh,
              currency,
              country,
              tags,
              image,
              seniority,
              other,
              active
              FROM jobs where id = $id
       """.query[Job].option.transact(xa)
  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         UPDATE jobs
         SET
         company = ${jobInfo.company},
         title = ${jobInfo.title},
         description = ${jobInfo.description},
         externalUrl = ${jobInfo.externalUrl},
         remote = ${jobInfo.remote},
         location = ${jobInfo.location},
         salaryLo = ${jobInfo.salaryLo},
         salaryHigh = ${jobInfo.salaryHigh},
         currency = ${jobInfo.currency},
         country = ${jobInfo.country},
         tags = ${jobInfo.tags},
         image = ${jobInfo.image},
         seniority = ${jobInfo.seniority},
         other = ${jobInfo.other}
         where id = ${id}
       """.update.run
      .transact(xa)
      .flatMap(_ => findById(id))
  override def delete(id: UUID): F[Int] =
    sql"""
         delete from jobs where id = ${id}
       """.update.run
      .transact(xa)
}

object LiveJobs {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] =
    new LiveJobs[F](xa).pure[F]
}
