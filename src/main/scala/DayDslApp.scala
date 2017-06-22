import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}


/**
  * Pragmatic Scala, page 86
  */
object DayDslApp extends App {

  printDate(2 days ago)

  printDate(5 days from_now)


  sealed trait TimeDirection

  object ago extends TimeDirection
  object from_now extends TimeDirection


  implicit class DaysDsl(n: Int) {

    def days(d: TimeDirection): LocalDate = d match {
      case `ago` => today.minusDays(n)
      case `from_now` => today.plusDays(n)
    }

    private def today = LocalDate.now
  }

  def printDate(date: LocalDate): Unit =
    println(
      DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(date)
    )
}
