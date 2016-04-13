package ru.art0.finhelper.components

import ru.art0.finhelper.components.PrimitiveParser.ParseException
import ru.art0.finhelper.models.{Record, Type}

trait RecordParser {
  def parse(lines: Seq[String]): Seq[Either[String, Record]]
}

trait RecordParserComponent {
  def recordParser: RecordParser
}

class RecordParserImpl extends RecordParser {
  this: PrimitiveParserComponent =>

  val RowDelimiter = ";"

  def parse(lines: Seq[String]): Seq[Either[String, Record]] = {
    var lineCounter = 0

    (for {
      line <- lines.map(_.trim) if line.nonEmpty
      columns = (line + RowDelimiter * 10).split(RowDelimiter, -1).take(8).map(_.trim).toList
    } yield {
      lineCounter += 1

      try {
        val List(date, amountCurrencyString, category, description, srcPurse, dstPurse, srcPurseBalance, dstPurseBalance) = columns

        val `type` = typeParser.parseRequired(category)

        val record =
          Record(
            lineCounter,
            `type`,
            categoryParser.parseRequired(category),
            dateParser.parseRequired(date),
            purseParser.parseWithDefaultCondition(srcPurse, true).get,
            purseParser.parseWithDefaultCondition(dstPurse, `type` == Type.Transfer),
            amountParser.parseRequired(amountCurrencyString),
            currencyParser.parseRequired(amountCurrencyString),
            stringParser.parse(description).getOrElse(""),
            amountParser.parse(srcPurseBalance),
            amountParser.parse(dstPurseBalance)
          )

        validate(record)

        Right(record)

      } catch {
        case e: ParseException => Left(e.getMessage + s", line $lineCounter: $line")
        case e: ValidationException => Left(e.getMessage + s", line $lineCounter: $line")
      }
    }).toList
  }

  private def validate(record: Record): Unit = {
    if (record.`type` == Type.Transfer && record.dstPurse.isEmpty) {
      throw new ValidationException("Transfer operation should have destination purse")
    } else if (record.`type` != Type.Transfer && record.dstPurse.nonEmpty) {
      throw new ValidationException("Only transfer operations allowed to have destination purse")
    } else if (record.`type` == Type.Delta && record.dstPurse.nonEmpty) {
      throw new ValidationException("Delta operation should not have a destination purse")
    } else if (record.`type` == Type.Delta && record.dstPurse.nonEmpty) {
      throw new ValidationException("Delta operation should not have a destination purse")
    } else if (record.dstBalance.isDefined && record.dstPurse.isEmpty) {
      throw new ValidationException("Destination purse balance is set while destination purse is empty")
    } else if (record.dstPurse.isDefined && record.srcPurse == record.dstPurse.get) {
      throw new ValidationException("Source and destination purses are equal")
    }
  }

  class ValidationException(m: String) extends RuntimeException(m)
}
