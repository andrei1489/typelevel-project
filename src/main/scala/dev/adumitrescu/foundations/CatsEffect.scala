package dev.adumitrescu.foundations

import cats.{Defer, MonadError}
import cats.effect.kernel.Fiber
import cats.effect.{Concurrent, Deferred, GenSpawn, IOApp, MonadCancel, Ref, Resource, Spawn, Sync, Temporal}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.concurrent.duration.*
import scala.util.Random

object CatsEffect extends IOApp.Simple {
  /*
    Describing computations as values
   */
  //IO = data structure describing arbitrary computations (including side effects)
  import cats.effect.IO
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO.apply {
    // complex code
    println("I am just about to compute the meaning of life")
    42
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global
    val meaningOfLife = io.unsafeRunSync()
    println(s"the result of the effect is $meaningOfLife")
  }

  //transformations
  // map + flatmap

  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO.println(mol))
  //for-comprehension
  def smallProgram(): IO[Unit] =
    for {
      line1 <- IO(StdIn.readLine())
      line2 <- IO(StdIn.readLine())
      _ <- IO(println(line1 + line2))
    } yield ()

  //old style of cats effect
  //  def main(args: Array[String]): Unit = {
  //    evaluateIO(smallProgram())
  //  }

  // raise/catch errors
  val failure: IO[Int] = IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = failure.handleErrorWith {
    case _: RuntimeException => IO(println("I'm still here, no worries"))
  }

  // fibers = "lightweight threads"
  // *> means flatmap
  val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))
  val manyPrints = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _ <- fib1.join
    _ <- fib2.join
  } yield ()

  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("i'm cancelled!"))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiver")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // uncancellation
  val ignoredCancellation = for {
    fib <- IO
      .uncancelable(_ => delayedPrint.onCancel(IO(println("i'm cancelled!"))))
      .start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiver")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // resources
  val readingResource = Resource.make(
    IO(
      scala.io.Source
        .fromFile("src/main/scala/dev/adumitrescu/foundations/CatsEffect.scala")
    )
  )(source => IO(println("closing source")) *> IO(source.close()))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  //composed resources
  val copiedFileResource = Resource.make(IO(new PrintWriter(new FileWriter(new File("src/main/resources/dumpedFile.scala")))))(writer => IO(println("closing duplicate file")) *> IO(writer.close()))

  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(destination.println))
  }

  // abstract kind of computations
  trait MyMonadCancel[F[_], E] extends MonadError[F,E] {
    trait CancellationFlagReseter {
      def apply[A](fa: F[A]): F[A] // with the cancellation flag reset
    }
    def canceled: F[Unit]
    def uncancellable[A](poll: CancellationFlagReseter => F[A]):F[A]
  }

  val monadCancelIO: MonadCancel[IO, Throwable] = MonadCancel[IO]
  val uncancelableIO = monadCancelIO.uncancelable(_ => IO(42)) // same as IO.uncanceable(...)

  // Spawn = ability to create fibers
  trait MyGenSpawn[F[_], E] extends MonadCancel[F,E] {
    def start[A](fa: F[A]): F[Fiber[F, E,A]] // creates a fiber
    //never, cede, racePair
  }

  trait MySpawn[F[_]] extends GenSpawn[F, Throwable]
  val spawnIO = Spawn[IO]
  val fiber = spawnIO.start(delayedPrint) // create a fiber, same as delayedPrint.start

  // Concurrent = concurrency primitives ( atomic references + promises )
  trait MyConcurrent[F[_]] extends Spawn[F] {
    def ref[A](a: A): F[Ref[F, A]]
    def deferred[A]: F[Deferred[F,A]]
  }

  // Temporal = ability to suspend computations for a given time

  trait MyTemporal[F[_]] extends Concurrent[F] {
    def sleep(time: FiniteDuration): F[Unit]
  }

  //Sync = ability to suspend synchronous arbitrary expressions in an effect
  trait MySync[F[_]] extends MonadCancel[F, Throwable] with Defer[F] {
    def delay[A](expression: => A):F[A]
    def blocking[A](expression: => A):F[A] // runs on a dedicated blocking thread pool
  }

  // Async = ability to suspend asynchronous computations (i.e. on other thread pools) into an effect managed by CE

  trait MyAsync[F[_]] extends Sync[F] with Temporal[F] {
    def executionContext: F[ExecutionContext]
    def async[A](cb: (Either[Throwable,A] => Unit) => F[Option[F[Unit]]]):F[A]
    
  }

  override def run: IO[Unit] = copyFileEffect
}
