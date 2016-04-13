package ru.art0.finhelper.models

import java.util.Currency

import org.joda.time.LocalDate

case class Record(line: Int,
                 `type`: Type.Type,
                 category: String,
                 dateAt: LocalDate,
                 srcPurse: Purse,
                 dstPurse: Option[Purse],
                 amount: Long,
                 currency: Currency,
                 comment: String,
                 srcBalance: Option[Long],
                 dstBalance: Option[Long],
                 isAutoBalance: Boolean = false
               )
