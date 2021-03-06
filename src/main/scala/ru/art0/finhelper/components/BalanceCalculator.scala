package ru.art0.finhelper.components

import ru.art0.finhelper.models._

import scala.collection.parallel.mutable
import scala.util.Try

trait BalanceCalculator {
  def calculateBalances(records: Seq[Record]): Seq[CalculatorResultItem]

  def generateStandardDeltas(record: Record, balances: BalancesMap): Seq[RecordWithSnapshot]
}

trait BalanceCalculatorComponent {
  def balanceCalculator: BalanceCalculator
}

class BalanceCalculatorImpl extends BalanceCalculator {
  this: ConfigurationComponent =>

  override def calculateBalances(records: Seq[Record]): Seq[CalculatorResultItem] = {
    val balances = new BalancesMap

    val nestedRecordsWithSnapshots =
      for (record <- records) yield {
        Try {

          var deltas = Seq.empty[RecordWithSnapshot]

          record.`type` match {
            case RecordType.Balance if balances.getBalance(record.srcPurse).isEmpty =>
              // First balance record
              balances.initPurse(record.srcPurse, record.amount, record.currency)
              Seq(Right(RecordWithSnapshot(record, balances.getSnapshot)))

            case RecordType.Balance =>
              // Subsequent balance records
              deltas = Seq(generateDeltaOpt(record, record.srcPurse, record.amount, balances)).flatten

            case RecordType.Transfer =>
              deltas = generateStandardDeltas(record, balances)
              balances.subtractPurseBalance(record.srcPurse, record.amount, record.currency)
              balances.subtractPurseBalance(record.dstPurse.get, -record.amount, record.currency)

            case _ =>
              deltas = generateStandardDeltas(record, balances)
              balances.subtractPurseBalance(record.srcPurse, record.amount, record.currency)
          }

          deltas.map(Right(_)) ++ Seq(Right(RecordWithSnapshot(record, balances.getSnapshot)))
        }.recover {
          case e => Seq(Left(e.getMessage))
        }.get
      }

    nestedRecordsWithSnapshots.flatten
  }

  override def generateStandardDeltas(record: Record, balances: BalancesMap): Seq[RecordWithSnapshot] = {
    var resultItems = Seq.empty[RecordWithSnapshot]

    for {
      srcBalance <- record.srcBalance
      delta <- generateDeltaOpt(record, record.srcPurse, srcBalance + record.amount, balances)
    } {
      resultItems :+= delta
    }

    for {
      dstPurse <- record.dstPurse
      dstBalance <- record.dstBalance
      delta <- generateDeltaOpt(record, dstPurse, dstBalance - record.amount, balances)
    } {
      resultItems :+= delta
    }

    resultItems
  }

  private def generateDeltaOpt(record: Record,
                                purse: Purse,
                                balanceAfterWithExpense: Long,
                                balances: BalancesMap): Option[RecordWithSnapshot] = {
    val deltaRecord =
      Record(
        line = record.line,
        `type` = RecordType.Delta,
        category = config.deltaCategories.head,
        srcPurse = purse,
        dstPurse = None,
        comment = "Autogenerated delta",
        srcBalance = None,
        dstBalance = None,
        amount = balances.getBalance(purse).fold(sys.error(s"""Unexpected purse: "$purse" """)) { balanceBefore =>
          balanceBefore.amount - balanceAfterWithExpense
        },
        dateAt = record.dateAt,
        currency = record.currency
      )

    balances.subtractPurseBalance(purse, deltaRecord.amount, deltaRecord.currency)

    if (deltaRecord.amount != 0) {
      Some(RecordWithSnapshot(deltaRecord, balances.getSnapshot))
    } else {
      None
    }
  }
}