package ru.art0.finhelper.components

import java.util.Currency

import org.joda.time.LocalDate
import ru.art0.finhelper.components.PrimitiveParser.ParseException
import ru.art0.finhelper.models.{Purse, RecordType}

trait PrimitiveParserComponent {
  def dateParser: DateParser
  def amountParser: AmountParser
  def currencyParser: CurrencyParser
  def typeParser: TypeParser
  def categoryParser: CategoryParser
  def stringParser: StringParser
  def purseParser: PurseParser
  def intParser: IntParser
}

trait PrimitiveParser[T] {
  final def parse(s: String): Option[T] = {
    s.trim match {
      case "" => None
      case _ => Some(doParse(s))
    }
  }

  // TODO: make more understandable mistakes information
  final def parseRequired(s: String): T = {
    parse(s).getOrElse(throw new ParseException(s"""Not found required value, parser: """ + this.getClass))
  }

  protected def doParse(s: String): T
}

class DateParser extends PrimitiveParser[LocalDate] {

  override protected def doParse(s:String): LocalDate = {
    s match {
      case PrimitiveParser.DatePattern(day, month) => new LocalDate(resolveYear(month.toInt), month.toInt, day.toInt)
      case _ => throw new ParseException(s"""Can not parse date "$s" """)
    }
  }

  private def resolveYear(month: Int): Int = {
    if (month <= LocalDate.now().getMonthOfYear)
      LocalDate.now().getYear
    else
      LocalDate.now().getYear - 1
  }
}

class AmountParser extends PrimitiveParser[Long] {

  override protected def doParse(s:String): Long = s match {
    case PrimitiveParser.AmountPattern(amount, modifier, currencyCode) =>
      val modifierValue =
        modifier match {
          case "K" | "К" => 1000
          case "M" | "М" => 1000000
          case "" => 1
          case _ => throw new ParseException(s"""Unexpected amount modifier value: "$modifier" """)
        }

      amount.toLong * modifierValue

    case _ => throw new ParseException(s"""Can not parse amount "$s" """)
  }
}

class CurrencyParser extends PrimitiveParser[Currency] {

  var defaultCurrency: Currency = Currency.getInstance("RUB")

  def setDefaultCurrency(currency: Currency) {
    defaultCurrency = currency
  }

  override protected def doParse(s:String): Currency = s match {
    case PrimitiveParser.AmountPattern(_, _, currencyCode) =>
      try {
        Currency.getInstance(
          currencyCode match {
            case "$" => "USD"
            case "р" => "RUB"
            case "€" => "EUR"
            case "" => defaultCurrency.getCurrencyCode
            case other => other
          }
        )
      } catch {
        case e: IllegalArgumentException => throw new ParseException(s"""Unexpected currency code: "$currencyCode" """)
      }

    case _ => throw new ParseException(s"""Can not parse amount "$s" """)
  }
}

class TypeParser extends PrimitiveParser[RecordType.RecordType] {
  this: ConfigurationComponent =>

  override protected def doParse(s:String): RecordType.RecordType = s match {
    case c if config.incomeCategories.contains(c) => RecordType.Income
    case c if config.transferCategories.contains(c) => RecordType.Transfer
    case c if config.deltaCategories.contains(c) => RecordType.Delta
    case c if config.balanceCategories.contains(c) => RecordType.Balance
    case _ => RecordType.Expense
  }
}

class CategoryParser extends PrimitiveParser[String] {
  this: ConfigurationComponent =>

  override protected def doParse(s:String): String = s match {
    case _ if config.validCategories.contains(s) => s
    case _ => throw new ParseException(s"""Unknown category: "$s" """)
  }
}

class StringParser extends PrimitiveParser[String] {
  override protected def doParse(s:String): String = s
}

class PurseParser extends PrimitiveParser[Purse] {
  this: ConfigurationComponent =>

  override protected def doParse(s:String): Purse = s match {
    case _ if config.validPurses.contains(s) => s
    case _ => throw new ParseException(s"""Unknown purse code: "$s" """)
  }

  def parseWithDefaultCondition(s: String, canBeDefault: Boolean): Option[Purse] = {
    if (canBeDefault) {
      Some(parse(s).getOrElse(config.defaultPurse))
    } else {
      parse(s)
    }
  }
}

class IntParser extends PrimitiveParser[Int] {
  override protected def doParse(s:String): Int = s.toInt
}


object PrimitiveParser {

  val DatePattern = """^(\d+)\.(\d+)$""".r
  val AmountPattern = """^(-?\d+)\s*([K|К]?)\s*((?:р|\$|€|[A-Za-z]{3})?)$""".r

  class ParseException(m: String) extends RuntimeException(m)

}