package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

object Deals extends Controller {

  def dealCityOverview() = Action {
    Async {
      supportedCities().map {
        citys =>
          val sortedCities = citys.toIndexedSeq.sortBy(_._1)
          Ok(views.html.index(sortedCities))
      }
    }
  }

  def topDeals(city: String) = Action {
    Ok(views.html.topdeal(city))
  }

  def topDealsTsv(city: String) = Action {
    Async {
      val res = dealOverviewData(city).map(_.flatten)
      res.map(util.asTsv).map(tsv => Ok(tsv))
    }
  }

  def dealOverviewData(city: String) = {
    dealLinksForCity(city).flatMap {
      links =>
        val listOfDealFutures = links.map(dealData)
        Future.sequence(listOfDealFutures)
    }
  }

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
}
