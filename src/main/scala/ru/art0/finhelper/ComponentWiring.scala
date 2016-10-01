package ru.art0.finhelper

import com.typesafe.config.{ConfigFactory, Config}
import ru.art0.finhelper.components._
import ru.art0.finhelper.services._

object ComponentWiring {

  lazy val configurationImpl = new Configuration {
    private lazy val envConfig = ConfigFactory.systemEnvironment()
    private lazy val fileConfig = ConfigFactory.load()

    override def underlying: Config = envConfig.withFallback(fileConfig)
  }
  trait ConfigurationComponentImpl extends ConfigurationComponent {
    override def config: Configuration = configurationImpl
  }

  lazy val balanceCalculatorImpl = new BalanceCalculatorImpl with ConfigurationComponentImpl
  trait BalanceCalculatorComponentImpl extends BalanceCalculatorComponent {
    override def balanceCalculator: BalanceCalculator = balanceCalculatorImpl
  }

  trait PrimitiveParserComponentImpl extends PrimitiveParserComponent {
    override lazy val dateParser = new DateParser

    override lazy val currencyParser = new CurrencyParser

    override lazy val categoryParser = new CategoryParser with ConfigurationComponentImpl

    override lazy val intParser = new IntParser

    override lazy val amountParser = new AmountParser

    override lazy val stringParser = new StringParser

    override lazy val typeParser = new TypeParser with ConfigurationComponentImpl

    override lazy val purseParser = new PurseParser with ConfigurationComponentImpl
  }

  lazy val recordParserImpl = new RecordParserImpl with PrimitiveParserComponentImpl
  trait RecordParserComponentImpl extends RecordParserComponent {
    override def recordParser: RecordParser = recordParserImpl
  }

  lazy val helperServiceImpl = new HelperServiceImpl with RecordParserComponentImpl with BalanceCalculatorComponentImpl
  trait HelperServiceComponentImpl extends HelperServiceComponent {
    override def helperService: HelperService = helperServiceImpl
  }

  lazy val tableRendererImpl = new TableRendererImpl
  trait TableRendererComponentImpl extends TableRendererComponent {
    override def tableRenderer: TableRenderer = tableRendererImpl
  }
}
