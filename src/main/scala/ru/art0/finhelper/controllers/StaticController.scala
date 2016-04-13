package ru.art0.finhelper.controllers

trait StaticController extends Controller {

  val route = {
    pathEndOrSingleSlash {
      getFromResource("public/index.html")
    } ~
      getFromResourceDirectory("public")
  }

}
