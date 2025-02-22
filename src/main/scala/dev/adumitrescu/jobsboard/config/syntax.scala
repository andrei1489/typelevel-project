package dev.adumitrescu.jobsboard.config

import cats.{MonadError, MonadThrow}
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](implicit reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {

        case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
        case Right(value) => F.pure(value)

      } // Either[Errors,A
}
