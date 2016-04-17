package ru.art0.finhelper.controllers

import play.api.libs.json.Json
import ru.art0.finhelper.components.ConfigurationComponent
import ru.art0.finhelper.services.{TableRendererComponent, TableRendererImpl, TableRenderer, HelperServiceComponent}
import spray.httpx.PlayJsonSupport._

trait FinHelperController extends Controller {

  this: HelperServiceComponent with TableRendererComponent with ConfigurationComponent =>

  implicit lazy val convertRequestReads = Json.reads[ConvertRequest]

  val route =
    pathPrefix("api") {
      (post & path("convert") & entity(as[ConvertRequest])) { request =>
        val lineSeq = helperService.convert(request.data)
        complete(tableRenderer.render(lineSeq))
      }
    }

  case class ConvertRequest(data: String)
}
