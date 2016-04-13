package ru.art0.finhelper.services

import ru.art0.finhelper.components.{BalanceCalculatorComponent, RecordParserComponent}
import ru.art0.finhelper.models._

trait HelperService {
  def convert(input: String): Seq[ConvertResultItem]
}

trait HelperServiceComponent {
  def helperService: HelperService
}

trait HelperServiceImpl extends HelperService {

  this: RecordParserComponent
    with BalanceCalculatorComponent =>

  val renderer: TableRenderer = new TableRendererImpl

  def convert(input: String): Seq[ConvertResultItem] = {
    val records = recordParser.parse(input.replace("\r\n", "\n").split("\n"))

    val errors = records.flatMap(_.left.toOption)
    val recordsWithBalance = balanceCalculator.calculateBalances(records.flatMap(_.right.toOption))

    errors.map(Left(_)) ++ recordsWithBalance
  }

  private def run() = {
    val Example = """
                    |1.10; 0; !статус;; сЯД
                    |1.10; 0; !статус;; сПСБ
                    |1.10; 0; !статус;; к
                    |1.10; 0; !статус;; сАБ
                    |1.10; 0; !статус;; кЗП
                    |1.10; 0; !статус;; кЗП2
                    |1.10; 0; !статус;; кБ3
                    |1.10; 0; !статус;; кК
                    |1.10; 400; ЕИ; телефон Химки; сЯД
                    |1.10; 370; ЕИ; интернет Химки 10; сПСБ
                    |1.10; 2000; квартира; электричество; сАБ
                    |1.10; 10810; квартира; квартплата Перово 07-09; сАБ
                    |1.10; 3610; ЕИ; квартплата Химки 08; сАБ;; 2995
                    |3.10; 760; продукты
                    |3.10; -50000; доходы; нал за 09 (еще должны ~2.5К); кЗП2
                    |4.10; 420; здоровье/лекарства; гомеопатия
                    |4.10; 630; отдых; с Дашей в граблях
                    |4.10; 630; отдых; с Дашей в граблях
                    |5.10; -10000; -;; кЗП; кЗП2; 10000; 120000
                    |5.10; -5000; -;;; кЗП2; 5350; 115000
                    |5.10; -40000; -; инвестиции от ЗП за 08-09; кБ3; кЗП2; 152000; 75000
                    |5.10; -55500; -; на дачу от ЗП за 09; кК; кЗП2; 60000; 19500
                    |6.10; 525; транспорт; маршрутка на 5 дней
                    |6.10; 580; еда; конфеты и бананы
                    |6.10; -10205; ЗП; безнал часть за 09; сПСБ;; 10210
                    |6.10; 615; продукты
                    |6.10; 1000; машина/другое; два штрафа за превышение на 20-40 км/ч; сАБ
                    |7.10; -8000; -; на оплату отдыха в санатории;; кЗП2
                    |7.10; 8300; отдых; в ДО Подмосковье в ноябре
                    |8.10; 710; продукты; творог
                    |9.10; 150; подарки; цветы Даше
                    |11.10; 300; отдых; в граблях с Дашей
                  """.stripMargin
  }

}

