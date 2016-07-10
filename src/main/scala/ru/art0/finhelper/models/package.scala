package ru.art0.finhelper

package object models {
  type Purse = String
  type BalanceSnapshot = Map[Purse, AmountCurrency]
  type CalculatorResultItem = Either[String, RecordWithSnapshot]
  type ConvertResultItem = CalculatorResultItem
}
