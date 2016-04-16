package ru.art0.finhelper.components

import com.typesafe.config.Config
import ru.art0.finhelper.models._

trait ConfigurationComponent {
  def config: Configuration
}

trait Configuration {

  import Configuration.Keys._

  def underlying: Config

  def httpHost = getOpt(globalPrefix("host")).getOrElse("localhost")
  def httpPort = getOpt(globalPrefix("port")).getOrElse("8080").toInt

  def pathPrefix = getOpt(globalPrefix("path-prefix")).getOrElse("")

  def incomeCategories: Seq[String] = toList(getOpt(IncomeCategories).getOrElse("доходы"))
  def transferCategories: Seq[String] = toList(getOpt(TransferCategories).getOrElse("-"))
  def deltaCategories: Seq[String] = toList(getOpt(DeltaCategories).getOrElse("дельта"))
  def balanceCategories: Seq[String] = toList(getOpt(BalanceCategories).getOrElse("баланс"))
  def validCategories: Seq[String] = toList(getRequired(ValidCategories))
  def validPurses: Seq[String] = toList(getRequired(ValidPurses))
  def defaultPurse: Purse = getOpt(DefaultPurse).getOrElse("к")

  private def getRequired(key: String): String =
    getOpt(key).getOrElse(sys.error(s"Configuration key '$key' is required"))

  private def getOpt(key: String): Option[String] = {
    if (underlying.hasPath(key)) {
      Some(underlying.getString(key))
    } else {
      None
    }
  }

  private def toList(s: String): Seq[String] = s.split(ValueSeparator).map(_.trim)
}

object Configuration {
  object Keys {

    val IncomeCategories = categories("income")
    val TransferCategories = categories("transfer")
    val DeltaCategories = categories("delta")
    val BalanceCategories = categories("balance")
    val ValidCategories = categories("valid")
    val ValidPurses = purses("valid")
    val DefaultPurse = purses("default")

    val ValueSeparator = ","

    def purses(name: String) = globalPrefix(s"purses.$name")
    def categories(name: String) = globalPrefix(s"categories.$name")
    def globalPrefix(name: String) = s"finhelper.$name"
  }
}


//val RowDelimiter = ";"
//val Delimiter = ","
//val ValidPurses = "к,сЯД,сПСБ,сАБ,кЗП2,кЗП,кБ3,кК".split(Delimiter)
//val ValidCategories = "!статус,-,еда,ЕИ,коммуналка/электроэнергия,коммуналка/квартплата,продукты,ЗП,здоровье/лекарства,отдых,транспорт,уда,машина/другое,подарки".split(Delimiter)
//val IncomeCategories = "доходы,ЗП".split(Delimiter)
//val TransferCategory = "-"
//val DeltaCategory = "дельта"
//val BalanceCategory = "!статус"
//val DefaultPurse = "к"
