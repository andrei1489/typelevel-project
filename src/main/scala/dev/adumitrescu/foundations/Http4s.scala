package dev.adumitrescu.foundations

import cats.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.ci.CIString
import org.http4s.ember.server.EmberServerBuilder

import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "Students" and "Courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String,
                    title: String,
                    year: Int,
                    students: List[Student],
                    instructorName: String)

  object CourseRepository {
    //a database
    private val catsEffectCource = Course(
      "be549b76-ba0f-4f02-b30f-e97d2078ced0",
      "Rock the JVM Ultimate scala course",
      2022,
      List("Daniel", "Yoda"),
      "Martin Odersky"
    )
    private val courses: Map[String, Course] = Map(
      catsEffectCource.id -> catsEffectCource
    )
    def findCoursesById(courseId: UUID) = courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  //esential REST endpoints
  //GET localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  //GET localhost:8080/courses/be549b76-ba0f-4f02-b30f-e97d2078ced0/students

  object InstructorQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher
      extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(
            maybeYear
          ) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) =>
            y.fold(
              _ => BadRequest("Parameter 'year' is invalid"),
              year => Ok(courses.filter(_.year == year).asJson)
            )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) =>
            Ok(
              students.asJson,
              Header.Raw(CIString("My-Custom-Header"), "rockthejvm")
            )
          case None => NotFound(s"No course with $courseId")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] =
    courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes =
    Router("/api" -> courseRoutes[IO], "/private" -> healthEndpoint[IO]).orNotFound
  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routerWithPathPrefixes)
      .build
      .use(_ => IO.println("Server ready") *> IO.never)

}
