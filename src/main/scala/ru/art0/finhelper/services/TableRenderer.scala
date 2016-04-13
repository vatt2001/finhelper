package ru.art0.finhelper.services

import java.text.{DecimalFormat, DecimalFormatSymbols}

import ru.art0.finhelper.models._

import scala.xml.{Unparsed, Elem}

trait TableRenderer {
  def render(rows: Seq[ConvertResultItem]): String
}

trait TableRendererComponent {
  def tableRenderer: TableRenderer
}

class TableRendererImpl extends TableRenderer {

  override def render(rows: Seq[ConvertResultItem]): String = {
    val balancePurses = extractBalancePurses(rows)
    val header = formatHeader(rows, balancePurses)
    val columnsQty = header.child.size

    renderTable(
      header,
      rows.map(either =>
        either.fold(
          row => formatErrorRow(columnsQty, row),
          row => formatSimpleRow(balancePurses, row)
        )
      )
    ).toString
  }

  private def extractBalancePurses(rows: Seq[ConvertResultItem]): Seq[Purse] = {
    val finalBalances = rows.filter(_.isRight).lastOption.fold(emptyBalance)(_.right.get._2)
    finalBalances.keys.toSeq
  }

  private def formatHeader(rows: Seq[ConvertResultItem], balancePurses: Seq[Purse]): Elem = {

    val balancePursesCells = balancePurses.map { purse =>
      <th>
        {purse}
      </th>
    }

    // TODO: foramt type as icon
    // TODO: format balances as title on purses

    <tr>
      <th class="">Date</th>
      <th class="">T</th>
      <th class="">Amount</th>
      <th class="">Category</th>
      <th class="">Comment</th>
      <th class="">Src</th>
      <th class="">Dst</th>
      <th class=""></th>{balancePursesCells}
    </tr>
  }

  private def formatSimpleRow(balancePurses: Seq[Purse], row: (Record, BalanceSnapshot)): Elem =
    row match {
      case (record, balance) =>

        val balanceCells =
          balancePurses.map(key =>
            balance.get(key).fold("")(b =>
              decimalFormat.format(b.amount).toString
            )
          ).map { value =>
            <td class="balance amount">{value}</td>
          }

        <tr>
          <td class="date">
            <span title={s"Line: ${record.line}"}>{record.dateAt.toString("dd.MM.yy")}</span>
          </td>
          <td class="type">
            {formatType(record.`type`)}
          </td>
          <td class="amount">
            {formatAmountCurrency(record)}
          </td>
          <td class="">
            {record.category}
          </td>
          <td class="">
            {record.comment}
          </td>
          <td class="purse">
            {formatPurse(record.srcPurse, record.srcBalance)}
          </td>
          <td class="purse">
            {formatPurse(record.dstPurse.getOrElse(""), record.dstBalance)}
          </td>
          <td class=""></td>{balanceCells}
        </tr>
    }

  private def renderTable(renderedHeader: Elem, renderedRows: Seq[Elem]): Elem = {
    <table class="table table-hover table-condensed">
      <thead>
        {renderedHeader}
      </thead>
      <tbody>
        {renderedRows}
      </tbody>
    </table>
  }

  private def formatErrorRow(columnsQty: Int, error: String): Elem = {
    <tr class="danger">
      <td colspan={columnsQty.toString}>
        {error}
      </td>
    </tr>
  }

  private def formatAmountCurrency(row: Record): Unparsed = {
    val value = decimalFormat.format(row.amount) + " " + currencyMap.getOrElse(row.currency.toString, row.currency.toString)
    Unparsed(value.replace(" ", "&nbsp;"))
  }

  private def formatType(operationType: Type.Type): Elem = {
    val icon =
      operationType match {
        case Type.Balance => "balance glyphicon glyphicon-piggy-bank"
        case Type.Delta => "delta glyphicon glyphicon-flash"
        case Type.Expense => "expense glyphicon glyphicon-minus"
        case Type.Income => "income glyphicon glyphicon-plus"
        case Type.Transfer => "transfer glyphicon glyphicon-transfer"
      }
    <span class={icon} aria-hidden="true"></span>
  }

  private def formatPurse(purse: Purse, balance: Option[Long]): Elem = {
    balance.fold {
      <span>{purse}</span>
    } { balance =>
      val title = s"Balance after: ${decimalFormat.format(balance)}"
      <span title={title}>{purse}</span>
    }
  }

  private lazy val currencyMap = Map("RUB" -> "\u20BD", "USD" -> "$", "EUR" -> "€")

  private lazy val decimalFormat = {
    val symbols = new DecimalFormatSymbols()
    symbols.setDecimalSeparator(' ')
    new DecimalFormat("###,###", symbols)
  }

  private lazy val emptyBalance: BalanceSnapshot = Map.empty[Purse, AmountCurrency]
}


