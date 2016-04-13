package ru.art0.finhelper.controllers

import spray.routing.{Route, HttpService}

trait Controller extends HttpService {
  def route: Route
}
