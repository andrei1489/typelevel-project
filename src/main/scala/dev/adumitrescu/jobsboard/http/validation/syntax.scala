package dev.adumitrescu.jobsboard.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import dev.adumitrescu.jobsboard.http.responses.*
import dev.adumitrescu.jobsboard.http.validation.validators.*
import dev.adumitrescu.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.typelevel.log4cats.Logger

object syntax {

  def validatedEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {

    extension (req: Request[F]) {
      def validate[A: Validator](
          serverLogicIfValid: A => F[Response[F]]
      )(using EntityDecoder[F, A]): F[Response[F]] = {
        req
          .as[A]
          .logError(e => s"Parsing payload failed: $e")
          .map(validatedEntity)
          .flatMap {
            case Valid(entity) => serverLogicIfValid(entity)
            case Invalid(errors) =>
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }
      }
    }
  }
}
