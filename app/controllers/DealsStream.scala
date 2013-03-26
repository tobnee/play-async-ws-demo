package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Enumeratee}
import play.api.libs.EventSource
import service.Groupon
import akka.util.Timeout
import concurrent.{ExecutionContext, Await, Future}
import ExecutionContext.Implicits.global

object DealsStream extends Controller {

  def dealFeedView(city: String) = Action {
    Ok(views.html.dealStream(city))
  }

  def dealFeed(city: String) = Action {
    val in: Enumeratee[(String, String), String] = Enumeratee.map[(String, String)] {
      case e => e.toString()
    }
    Ok.feed(dealEvents(city) &> in ><> EventSource()).as("text/event-stream")
  }

  def dealEvents(city: String): Enumerator[(String, String)] = {
    val deals = Groupon.dealLinksForCity(city).map(_.map(Groupon.dealData))
    val d = Await.result(deals.fallbackTo(Future(Nil)), Timeout(1000).duration).toIterator
    Enumerator.generateM[(String, String)] {
      if (d.hasNext) d.next().map(_.orElse(Some("", "")))
      else Future(None)
    }
  }
}
