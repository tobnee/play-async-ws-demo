package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Enumeratee}
import play.api.libs.EventSource
import service.Groupon
import akka.util.Timeout
import concurrent.{ExecutionContext, Await, Future}
import ExecutionContext.Implicits.global
import play.api.libs.json.{JsValue, Json}
import Json._

object DealsStream extends Controller {
  val asJson: Enumeratee[(String, String), JsValue] = Enumeratee.map[(String, String)] {
    case (desc, percent) => toJson(Map("desc" -> toJson(desc), "percent" -> toJson(percent)))
  }

  def dealFeedView() = Action {
    Ok(views.html.dealStream())
  }

  def dealFeed() = Action {
    Async {
      buildCityEventProducers.map(enum => Ok.feed(enum through asJson.compose(EventSource())).as("text/event-stream"))
    }
  }

  def buildCityEventProducers: Future[Enumerator[(String, String)]] = {
    Groupon.supportedCities().map(a =>
      a.map {
        case (city, link) =>
          link.split("/").last
      }.map(dealEvents).reduce {
        (a, b) =>
          a >- b
      }
    )
  }

  def dealEvents(city: String): Enumerator[(String, String)] = {
    val deals = Groupon.dealLinksForCity(city).map(_.toStream.map(Groupon.dealData))
    val d = Await.result(deals.fallbackTo(Future(Stream())), Timeout(5000).duration).toIterator
    Enumerator.generateM {
      if (d.hasNext) {
        // build empty event if no desc is given
        val f = d.next().map(_.orElse(Some("", "")))
        // time messages to be delivered with a maximum rate
        f.flatMap(content => play.api.libs.concurrent.Promise.timeout(content, (20000*math.random).toLong))
      }
      else Future(None)
    }
  }
}
