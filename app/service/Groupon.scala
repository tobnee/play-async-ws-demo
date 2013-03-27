package service

import play.api.libs.ws.WS
import concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.api.Logger
import play.api.cache.Cache
import util._

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

  def getBody(path: String) = {
    Logger.debug(s"GET $path")
    WS.url(gpnLink(path)).get().map(_.body.toString)
  }

   def dealOverviewData(city: String) = {
    Groupon.dealLinksForCity(city).flatMap {
      links =>
        val listOfDealFutures = links.map(Groupon.cachedDealData)
        Future.sequence(listOfDealFutures)
    }
  }

 def gpnLink(path : String) = "http://www.groupon.de" + path
}
