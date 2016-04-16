package ru.art0.finhelper

import akka.actor.{ActorRefFactory, Actor, ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import ru.art0.finhelper.ComponentWiring.{ConfigurationComponentImpl, TableRendererComponentImpl, HelperServiceComponentImpl}
import ru.art0.finhelper.controllers.{Controller, StaticController, FinHelperController}
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object Boot extends App {

  implicit val system = ActorSystem("global")

  val service = system.actorOf(Props[FinHelperActor], "finhelper")

  implicit val timeout = Timeout(5.seconds)

  val config = ComponentWiring.configurationImpl

  IO(Http) ? Http.Bind(service, interface = config.httpHost, port = config.httpPort)
}

class FinHelperActor extends Actor with Controller {

  def actorRefFactory = context

  def receive = runRoute(route)

  lazy val route =
    logRequest("FIN_HELPER", Logging.InfoLevel) {
      controllers.view.map(_.route).reduce(_ ~ _)
    }

  val controllers = Seq(
    new FinHelperController with HelperServiceComponentImpl with TableRendererComponentImpl with ConfigurationComponentImpl {
      override implicit def actorRefFactory: ActorRefFactory = context
    },
    new StaticController with ConfigurationComponentImpl {
      override implicit def actorRefFactory: ActorRefFactory = context
    }
  )
}
