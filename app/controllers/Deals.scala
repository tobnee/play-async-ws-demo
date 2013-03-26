package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import concurrent.{Await, Future, ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.libs.iteratee.{Enumeratee, Enumerator}
import org.apache.http.conn.params.ConnManagerParams
import akka.util.Timeout
import play.api.libs.EventSource

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

  def dealFeedView(city:String) = Action {
    Ok(views.html.dealStream(city))
  }

  def dealFeed(city: String) = Action {
    val in: Enumeratee[(String, String), String] = Enumeratee.map[(String,String)] {
      case e => e.toString()
    }
    Ok.feed(dealEvents(city) &> in ><> EventSource()).as("text/event-stream")
  }

  def dealEvents(city:String): Enumerator[(String,String)] = {
    val deals = dealLinksForCity(city).map { _.map(dealData) }
    val d = Await.result(deals.fallbackTo(Future(Nil)), Timeout(1000).duration).toIterator
    Enumerator.generateM[(String,String)] {
      if(d.hasNext) d.next().map(_.orElse(Some("","")))
      else Future(None)
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
