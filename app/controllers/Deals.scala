package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

object Deals extends Controller {
  def deal(id:Int) = Action{
    Ok(s"Deal $id is nice")
  }

  def topdeal(id:Int) = Action{
    if(id > 0 && id <=20) Ok(s"Deal $id is nice")
    else BadRequest("there are only 20 top deals")
  }


  def dealOverview(city:String) = Action {
    Async{
      val res = dealOverviewData(city)
      res.map(list => Ok(views.html.topdeal(Nil, city)))
    }
  }

  def dealOverviewTsv(city:String) = Action {
    Async{
      val res = dealOverviewData(city)
      res.map(asTsv).map(tsv => Ok(tsv))
    }
  }

  def asTsv(list:List[String]) = {
    val header = "letter\tfrequency\n"
    list.map(input => input.take(5)+"\t"+math.random.toString().drop(1))
      .mkString(header,"\n","")
  }

  def dealOverviewData(city: String) = {
    val links = dealLinksForCity(city).map(util.getDealLinks)
    links.flatMap {
      lin =>
        val futereContentList = lin.map {
          link =>
            dealData(link)
        }
        Future.sequence(futereContentList)
    }
  }

  def dealLinksForCity(city:String): Future[String] = {
    WS.url("http://www.groupon.de/deals/"+city).get().map {
      req => req.body.toString()
    }
  }

  def dealData(link: String): Future[String] = {
    WS.url("http://www.groupon.de"+link).get().map {
      req =>
        util.data(req.body.toString()).getOrElse("no desc")
    }
  }
}
