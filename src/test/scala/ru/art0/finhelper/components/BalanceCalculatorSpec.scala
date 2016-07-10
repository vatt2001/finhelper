package ru.art0.finhelper.components

import java.util.Currency

import org.joda.time.LocalDate
import org.scalatest._
import ru.art0.finhelper.models._
import ru.art0.finhelper.test.FakeConfigurationComponent

class BalanceCalculatorSpec extends FlatSpec with Matchers {

  "calculateBalances" should "do nothing for empty records set" in {
    calculator.calculateBalances(Seq.empty) shouldBe Seq.empty
  }

  it should "init balance for Balance records" in {
    val records = Seq(
      SampleBalanceRecord
    )

    val result = calculator.calculateBalances(records)

    result.size shouldBe 1
    assert(result.head.isRight, s"Unexpected error: ${result.head.left}")

    result.head.right.get.record shouldBe SampleBalanceRecord
    result.head.right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleBalanceRecord.amount, SampleBalanceRecord.currency)
    )
  }

  it should "generate error on updating balances for unknown purse" in {
    val records = Seq(
      SampleExpenseRecord
    )

    val result = calculator.calculateBalances(records)

    result.size shouldBe 1
    result.head.isRight should be (false)
    result.head.left.get should include ("Unexpected purse")
  }

  it should "update balances for Expense records without known balance after operation" in {
    val records = Seq(
      SampleBalanceRecord,
      SampleExpenseRecord.copy(srcBalance = None)
    )

    val result = calculator.calculateBalances(records)

    result.size shouldBe 2
    result(0).isRight should be (true)
    result(1).isRight should be (true)
    result(1).right.get.record shouldBe SampleExpenseRecord.copy(srcBalance = None)
    result(1).right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleBalanceRecord.amount - SampleExpenseRecord.amount, SampleBalanceRecord.currency)
    )
  }

  it should "update balances and generate deltas for Expense records" in {
    val records = Seq(
      SampleBalanceRecord,
      SampleExpenseRecord
    )

    val result = calculator.calculateBalances(records)

    // balance before: 100
    // expense: 5
    // balance after: 10
    // delta: balance before - expense - balance after
    val expectedDeltaRecord =
      DeltaRecordTemplate.copy(
        srcPurse = SrcPurse,
        amount = SampleBalanceRecord.amount - SampleExpenseRecord.amount - SampleExpenseRecord.srcBalance.get
      )

    result.size shouldBe 3
    result(0).isRight should be (true)
    result(1).isRight should be (true)
    result(2).isRight should be (true)

    result(1).right.get.record shouldBe expectedDeltaRecord
    result(2).right.get.record shouldBe SampleExpenseRecord

    result(1).right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleExpenseRecord.srcBalance.get + SampleExpenseRecord.amount, SampleBalanceRecord.currency)
    )
    result(2).right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleExpenseRecord.srcBalance.get, SampleBalanceRecord.currency)
    )
  }

  it should "update balances and generate deltas for Income records" in {
    val records = Seq(
      SampleBalanceRecord,
      SampleIncomeRecord
    )

    val result = calculator.calculateBalances(records)

    // balance before: 100
    // income: -5
    // balance after: 10
    // delta: balance before - income - balance after
    val expectedDeltaRecord =
      DeltaRecordTemplate.copy(
        srcPurse = SrcPurse,
        amount = SampleBalanceRecord.amount - SampleIncomeRecord.amount - SampleIncomeRecord.srcBalance.get
      )

    result.size shouldBe 3
    result(0).isRight should be (true)
    result(1).isRight should be (true)
    result(2).isRight should be (true)

    result(1).right.get.record shouldBe expectedDeltaRecord
    result(2).right.get.record shouldBe SampleIncomeRecord

    result(1).right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleIncomeRecord.srcBalance.get + SampleIncomeRecord.amount, SampleBalanceRecord.currency)
    )
    result(2).right.get.balanceSnapshot shouldBe Map(
      SampleBalanceRecord.srcPurse -> AmountCurrency(SampleIncomeRecord.srcBalance.get, SampleBalanceRecord.currency)
    )
  }

  it should "update both balances and generate deltas for Transfer records" in {
    val records = Seq(
      SampleBalanceRecord,
      SampleBalanceRecord.copy(srcPurse = DstPurse),
      SampleTransferRecord
    )

    val result = calculator.calculateBalances(records)

    val expectedSrcDeltaRecord =
      DeltaRecordTemplate.copy(
        srcPurse = SrcPurse,
        amount = SampleBalanceRecord.amount - SampleTransferRecord.amount - SampleTransferRecord.srcBalance.get
      )

    val expectedDstDeltaRecord =
      DeltaRecordTemplate.copy(
        srcPurse = DstPurse,
        amount = SampleBalanceRecord.amount + SampleTransferRecord.amount - SampleTransferRecord.dstBalance.get
      )

    result.size shouldBe 5
    result.foreach(_.isRight should be (true))

    result(2).right.get.record shouldBe expectedSrcDeltaRecord
    result(3).right.get.record shouldBe expectedDstDeltaRecord
    result(4).right.get.record shouldBe SampleTransferRecord

    result(1).right.get.balanceSnapshot shouldBe Map(
      SrcPurse -> AmountCurrency(SampleBalanceRecord.amount, SampleCurrency),
      DstPurse -> AmountCurrency(SampleBalanceRecord.amount, SampleCurrency)
    )
    result(2).right.get.balanceSnapshot shouldBe Map(
      SrcPurse -> AmountCurrency(SampleBalanceRecord.amount - expectedSrcDeltaRecord.amount, SampleCurrency),
      DstPurse -> AmountCurrency(SampleBalanceRecord.amount, SampleCurrency)
    )
    result(3).right.get.balanceSnapshot shouldBe Map(
      SrcPurse -> AmountCurrency(SampleBalanceRecord.amount - expectedSrcDeltaRecord.amount, SampleCurrency),
      DstPurse -> AmountCurrency(SampleBalanceRecord.amount - expectedDstDeltaRecord.amount, SampleCurrency)
    )
    result(4).right.get.balanceSnapshot shouldBe Map(
      SrcPurse -> AmountCurrency(SampleTransferRecord.srcBalance.get, SampleCurrency),
      DstPurse -> AmountCurrency(SampleTransferRecord.dstBalance.get, SampleCurrency)
    )
  }

  "generateDeltaOpt" should "generate nothing for record without balance" in {
    val map = new BalancesMap
    val record = SampleTransferRecord.copy(srcBalance = None)

    val result = calculator.generateDeltaOpt(record, record.srcPurse, map)

    result shouldBe None
  }

  it should "generate delta for Src purse" in {
    val map = new BalancesMap
    val record = SampleTransferRecord
    val srcBalanceMapAmount = 100
    map.initPurse(record.srcPurse, srcBalanceMapAmount, SampleCurrency)
    val expectedDelta = DeltaRecordTemplate.copy(
      line = record.line,
      srcPurse = record.srcPurse,
      amount = -(SrcBalance + record.amount - srcBalanceMapAmount)
    )
    val expectedSnapshot: BalanceSnapshot = Map(
      record.srcPurse -> AmountCurrency(record.amount + record.srcBalance.get, SampleCurrency)
    )

    val result = calculator.generateDeltaOpt(record, record.srcPurse, map)

    result.isDefined shouldBe true
    result.get.record shouldBe expectedDelta
    result.get.balanceSnapshot shouldBe expectedSnapshot
  }

  it should "generate delta for Dst purse" in {

    // balanceMap: 100
    // expected delta: -85 (record.amount 85)
    // operation: +5 (record.amount 5)
    // balanceAfter: 20

    val map = new BalancesMap
    val record = SampleTransferRecord
    val dstBalanceMapAmount = 100
    map.initPurse(record.dstPurse.get, dstBalanceMapAmount, SampleCurrency)
    val expectedDelta = DeltaRecordTemplate.copy(
      line = record.line,
      srcPurse = record.dstPurse.get,
      amount = -(DstBalance - record.amount - dstBalanceMapAmount)
    )
    val expectedSnapshot: BalanceSnapshot = Map(
      record.dstPurse.get -> AmountCurrency(-record.amount + record.dstBalance.get, SampleCurrency)
    )

    val result = calculator.generateDeltaOpt(record, record.dstPurse.get, map)

    result.isDefined shouldBe true
    result.get.record shouldBe expectedDelta
    result.get.balanceSnapshot shouldBe expectedSnapshot
  }

  it should "generate error if BalanceMap does not contain the purse" in {
    val map = new BalancesMap
    val record = SampleTransferRecord

    // TODO: use proper test library method
    try {
      calculator.generateDeltaOpt(record, record.srcPurse, map)
      fail("Exception should be thrown")
    } catch {
      case e: RuntimeException =>
    }
  }

  lazy val SrcPurse = "src"
  lazy val DstPurse = "dst"
  lazy val SrcBalance = 10
  lazy val DstBalance = 20
  lazy val SampleCurrency = Currency.getInstance("RUB")

  lazy val DeltaRecordTemplate =
    Record(
      42,
      RecordType.Delta,
      "дельта",
      LocalDate.now(),
      "",
      None,
      0,
      SampleCurrency,
      "Autogenerated delta",
      None,
      None,
      isAutoBalance = false
    )

  lazy val SampleTransferRecord =
    Record(
      42,
      RecordType.Transfer,
      "",
      LocalDate.now(),
      SrcPurse,
      Some(DstPurse),
      5,
      SampleCurrency,
      "",
      Some(SrcBalance),
      Some(DstBalance),
      isAutoBalance = false
    )

  lazy val SampleExpenseRecord =
    Record(
      42,
      RecordType.Expense,
      "",
      LocalDate.now(),
      SrcPurse,
      None,
      5,
      SampleCurrency,
      "Sample",
      Some(SrcBalance),
      None,
      isAutoBalance = false
    )

  lazy val SampleIncomeRecord = SampleExpenseRecord.copy(`type` = RecordType.Income, amount = -5)

  lazy val SampleBalanceRecord =
    Record(
      42,
      RecordType.Balance,
      "",
      LocalDate.now(),
      SrcPurse,
      None,
      100,
      SampleCurrency,
      "Sample",
      None,
      None,
      isAutoBalance = false
    )

  lazy val calculator = new BalanceCalculatorImpl with FakeConfigurationComponent {
    override def configMap: Map[String, String] = Map.empty
  }
}
