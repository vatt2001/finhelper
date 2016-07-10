package ru.art0.finhelper.models

import java.util.Currency
import scala.collection.mutable

class BalancesMap {
  private val balances: mutable.Map[Purse, AmountCurrency] = mutable.Map.empty
  private val bannedPurses: mutable.Set[Purse] = mutable.Set.empty

  def subtractPurseBalance(purse: Purse, difference: Long, currency: Currency): Unit = {
    getBalance(purse).fold {
      sys.error(s"""Error updating balance for purse "$purse": purse does not have initial balance""")
    } { prevBalance =>
      if (prevBalance.currency == currency && !bannedPurses.contains(purse)) {
        balances.update(purse, AmountCurrency(prevBalance.amount - difference, prevBalance.currency))
      } else if (prevBalance.currency == currency && bannedPurses.contains(purse)) {
        sys.error(s"""Operation with purse "$purse" was skipped as it had errors previously""")
      } else {
        bannedPurses.add(purse)
        sys.error(s"Unexpected purse currency. Expected: ${prevBalance.currency}, found: $currency")
      }
    }
  }

  def getBalance(purse: Purse): Option[AmountCurrency] = balances.get(purse)

  def getBannedPurses: Set[Purse] = bannedPurses.toSet

  def initPurse(purse: Purse, initialBalance: Long, currency: Currency): Unit = {
    getBalance(purse).fold {
      balances.put(purse, AmountCurrency(initialBalance, currency))
    } { _ =>
      bannedPurses.add(purse)
      sys.error(s"""Balance for purse "$purse" has already been initialized""")
    }
  }

  def getSnapshot: BalanceSnapshot = balances.toMap
}
