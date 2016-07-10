package ru.art0.finhelper.test

import scala.collection.JavaConversions._
import com.typesafe.config.{ConfigFactory, Config}
import ru.art0.finhelper.components.{ConfigurationComponent, Configuration}

trait FakeConfigurationComponent extends ConfigurationComponent {

  def configMap: Map[String, String]

  override def config: Configuration = new Configuration {
    override def underlying: Config = ConfigFactory.parseMap(configMap)
  }
}
