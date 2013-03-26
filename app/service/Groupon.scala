package service

import play.api.libs.ws.WS
import concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Groupon {
  def supportedCities() = {
    getBody("/").map(util.GrouponParser.extractCityLinks)
  }

  def dealLinksForCity(city: String) = {
    getBody("/deals/"+city).map(util.GrouponParser.extractDealLinks)
  }

  def dealData(link: String) = {
    getBody(link).map(util.GrouponParser.extractDealData)
  }

  def getBody(path: String) = {
    WS.url(util.gpnLink(path)).get().map(_.body.toString)
  }

   def dealOverviewData(city: String) = {
    Groupon.dealLinksForCity(city).flatMap {
      links =>
        val listOfDealFutures = links.map(Groupon.dealData)
        Future.sequence(listOfDealFutures)
    }
  }
}
