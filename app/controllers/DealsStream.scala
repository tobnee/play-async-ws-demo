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

  def dealFeedView(city: String) = Action {
    Ok(views.html.dealStream(city))
  }

  def dealFeed(city: String) = Action {
    val asJson: Enumeratee[(String, String), JsValue] = Enumeratee.map[(String, String)] {
      case (desc, percent) => toJson(Map("desc" -> toJson(desc), "percent" -> toJson(percent)))
    }
    Ok.feed(dealEvents(city) through asJson.compose(EventSource())).as("text/event-stream")
  }

  def dealEvents(city: String): Enumerator[(String, String)] = {
    val deals = Groupon.dealLinksForCity(city).map(_.map(Groupon.dealData))
    val d = Await.result(deals.fallbackTo(Future(Nil)), Timeout(1000).duration).toIterator
    Enumerator.generateM[(String, String)] {
      println("push event")
      if (d.hasNext) {
        val f = d.next().map(_.orElse(Some("", "")))
        f.flatMap(content => play.api.libs.concurrent.Promise.timeout(content, 500))
      }
      else Future(None)
    }
  }
}
