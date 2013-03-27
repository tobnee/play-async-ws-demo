package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Enumeratee}
import play.api.libs.EventSource
import service.Groupon
import akka.util.Timeout
import concurrent.{ExecutionContext, Await, Future}
import ExecutionContext.Implicits.global
import play.api.libs.json.Json
import Json._
import play.api.cache.Cache
import play.api.Logger

object DealsStream extends Controller {

  type DescPercent = (String,String)

  val asJson = Enumeratee.map[DescPercent] {
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

  def buildCityEventProducers: Future[Enumerator[DescPercent]] = {
    Groupon.supportedCities().map(a =>
      a.map{ case (city, link) => link.split("/").last }
        .map(dealEvents).reduce { (a, b) => a >- b }
    )
  }

  def dealEvents(city: String): Enumerator[DescPercent] = {
    val deals = Groupon.dealLinksForCity(city).map(_.toStream.map(cachedDealData))
    val d = Await.result(deals.fallbackTo(Future(Stream())), Timeout(5000).duration).toIterator
    Enumerator.generateM {
      if (d.hasNext) {
        Logger.debug(s"push event for $city")
        // build empty event if no desc is given
        val f = d.next().map(_.orElse(Some("", "")))
        // time messages to be delivered with a maximum rate
        f.flatMap(content => play.api.libs.concurrent.Promise.timeout(content, (20000*math.random).toLong))
      }
      else {
        Logger.debug(s"end stream for $city")
        Future(None)
      }
    }
  }

  def cachedDealData(link:String) = {
    import play.api.Play.current
    val res = Cache.getAs[Option[DescPercent]](link)
    if(!res.isDefined) {
      val f = Groupon.dealData(link)
      f.onSuccess{ case e => Cache.set(link,e,10000)}
      f
    } else {
      Logger.debug(s"load $link from cache")
      Future(res.get)
    }
  }
}
