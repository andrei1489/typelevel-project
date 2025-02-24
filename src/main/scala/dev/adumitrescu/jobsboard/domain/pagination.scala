package dev.adumitrescu.jobsboard.domain

import dev.adumitrescu.jobsboard.config.PaginationConfig

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    
    def apply(config: PaginationConfig)(maybeLimit: Option[Int], maybeOffset: Option[Int]) =
      new Pagination(maybeLimit.getOrElse(config.defaultPageSize), maybeOffset.getOrElse(0))
  }
}
