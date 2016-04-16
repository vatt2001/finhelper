package ru.art0.finhelper.controllers

import ru.art0.finhelper.components.ConfigurationComponent

trait StaticController extends Controller {

  this: ConfigurationComponent =>

  val route =
    rawPathPrefix(config.pathPrefix) {
      pathEndOrSingleSlash {
        getFromResource("public/index.html")
      } ~
        getFromResourceDirectory("public")
    }

}
