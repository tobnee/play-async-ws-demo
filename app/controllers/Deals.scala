package controllers

import play.api.mvc._
import concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import service.Groupon

object Deals extends Controller {

  def dealCityOverview() = Action {
    Async {
      Groupon.supportedCities().map {
        cities =>
          val sortedCities = cities.toIndexedSeq.sortBy(_._1)
          Ok(views.html.index(sortedCities))
      }
    }
  }

  def topDeals(city: String) = Action {
    Ok(views.html.topdeal(city))
  }

  def topDealsTsv(city: String) = Action {
    Async {
      val res = Groupon.dealOverviewData(city).map(_.flatten)
      res.map(asTsv).map(tsv => Ok(tsv))
    }
  }

  def asTsv(list: Traversable[(String,String)]) = {
    val header = "desc\tdiscount\tlongdesc\n"
    list.map{ case (desc,percent) => desc.take(7) + "\t" + percent + "\t" + desc}
      .mkString(header, "\n", "")
  }
 }
