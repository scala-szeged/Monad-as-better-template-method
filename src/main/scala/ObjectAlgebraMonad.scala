import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Based on the following: https://github.com/hablapps/bypassingfreemonads
  *
  * See also: https://blog.hablapps.com/?s=monad
  */
//noinspection TypeAnnotation, LanguageFeature
object ObjectAlgebraMonad extends App {


  trait IO[P[_]] {

    def read(): P[String]

    def log(input: String): P[Unit]

    def calculate(sentence: String): P[String]
  }

  object IO {

    object Syntax {

      def read[P[_]]()(implicit IO: IO[P]) = IO.read()

      def log[P[_]](input: String)(implicit IO: IO[P]) = IO.log(input)

      def calculate[P[_]](input: String)(implicit IO: IO[P]) = IO.calculate(input)
    }

  }


  trait Monad[P[_]] {

    def flatMap[A, B](p1: P[A])(f: A => P[B]): P[B]

    def returns[A](a: A): P[A]
  }


  object Monad {

    object Syntax {

      implicit class FlatMapOps[P[_], A](p: P[A])(implicit M: Monad[P]) {

        def flatMap[B](f: A => P[B]): P[B] = M.flatMap(p)(f)

        def map[B](f: A => B): P[B] = M.flatMap(p)(f andThen M.returns)
      }

      //def returns[P[_], A](a: A)(implicit M: Monad[P]) = M.returns(a)
    }

  }


  import IO.Syntax._, Monad.Syntax._
  // N E E D E D :  Monad.Syntax._

  /**
    * Using for-comprehensions, we are now able to log our program with the
    * look-and-feel of a conventional imperative program.
    */
  def loggedCalculation[P[_] : IO : Monad](): P[String] = {
    val result = for {
      input <- read()
      _ <- log(input)
      result <- calculate(input)
    } yield result

    result

    /*
        import scala.reflect.runtime.universe._

        println(show {
          reify {
            for {
              input <- read()
              _ <- log(input)
              result <- calculate(input)
            } yield result
          }
        })

        result
    */

    /*
    This is the result of reify:

        Expr[P[String]](Monad.Syntax.FlatMapOps(IO.Syntax.read()).flatMap(
          ((input) => Monad.Syntax.FlatMapOps(IO.Syntax.log(input)).flatMap(
            ((_) => Monad.Syntax.FlatMapOps(IO.Syntax.calculate(input)).map(
              ((result) => result)))))))
    */
  }

  // https://docs.google.com/presentation/d/1lbRuaIun8IOpw0hM52Amj6N-V25Uu52udRoYx2RBWJc/edit#slide=id.g17ca778fd0_0_1091
  // https://github.com/hablapps/bypassingfreemonads
  // https://github.com/hablapps/gist


  implicit object OptionMonad extends Monad[Option] {
    var count = 1

    override def flatMap[A, B](p1: Option[A])(f: (A) => Option[B]): Option[B] = {
      println(s"OptionMonad step $count intermediate result: $p1")
      count += 1
      p1.flatMap(f)
    }

    override def returns[A](a: A): Option[A] =
      Some(a)
  }

  implicit object OptionIO extends IO[Option] {
    override def read(): Option[String] =
      Some("getListOfPrimesTo 1000")

    override def log(input: String): Option[Unit] =
      Some {
        println(s"OptionIO log: We will calculate: $input")
      }

    def calculate(sentence: String): Option[String] = {
      val n = sentence.split("To ")(1).toInt

      // https://stackoverflow.com/questions/26287604/get-list-of-primes-to-n

      lazy val ps: Stream[Int] = 2 #:: Stream.from(3)
        .filter(x => ps.takeWhile(p => p * p <= x).forall(x % _ != 0))

      val last10 = ps.takeWhile(_ <= n).toList.takeRight(10)
      Some(last10.toString)
    }
  }

  println()
  println(s"loggedCalculation[Option] = ${loggedCalculation[Option]()}")


  import scala.concurrent.ExecutionContext.Implicits.global

  implicit object Mof extends Monad[Future] {
    def flatMap[A, B](p1: Future[A])(f: A => Future[B]): Future[B] =
      p1.flatMap(f)

    def returns[A](a: A): Future[A] = Future.successful(a)
  }

  implicit object AssyncIO extends IO[Future] {
    def read(): Future[String] = Future.successful("read from web service")


    def log(input: String): Future[Unit] = Future.successful {
      println(s"AssyncIO log: $input")
    }

    def calculate(sentence: String): Future[String] = Future.successful(sentence)
  }

  println
  loggedCalculation[Future]() onComplete {
    case Success(r) =>
      println(s"loggedCalculation[Future] = $r")

    case Failure(t) =>
      System.err.println("An error has occured: ")
      rootCause(t).printStackTrace(System.err)
  }
  Thread.sleep(1000)

  def rootCause(t: Throwable): Throwable = t.getCause match {
    case null => t
    case _ => rootCause(t.getCause)
  }

  //import cats.Id // Identity, encoded as `type Id[A] = A`, a convenient alias to make identity instances well-kinded.
  type Id[A] = A

  implicit object Mo extends Monad[Id] {
    override def flatMap[A, B](p1: Id[A])(f: (A) => Id[B]): Id[B] =
      f(p1)

    override def returns[A](a: A): Id[A] =
      a
  }

  implicit object ConsoleIO extends IO[Id] {
    override def read(): Id[String] =
      "Id ok"

    override def log(input: String): Id[Unit] =
      println(s"ConsoleIO log: $input")

    def calculate(sentence: String): Id[String] = sentence
  }

  println()
  println(s"loggedCalculation[Id] = ${loggedCalculation[Id]()}")
}
