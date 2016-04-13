package ru.art0.finhelper.models

object Type extends Enumeration {
  type Type = Value

  val Income = Value("income")
  val Expense = Value("expense")
  val Transfer = Value("transfer")
  val Delta = Value("delta")
  val Balance = Value("balance")
}
