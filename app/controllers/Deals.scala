package controllers

import play.api.mvc._
import concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import service.Groupon


object Deals extends Controller {

  def dealCityOverview() = Action {
    Async {
      Groupon.supportedCities().map {
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
      val res = Groupon.dealOverviewData(city).map(_.flatten)
      res.map(util.asTsv).map(tsv => Ok(tsv))
    }
  }

}
