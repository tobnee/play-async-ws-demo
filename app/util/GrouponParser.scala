package util

import java.io.StringReader
import org.joda.time.DateTime

object GrouponParser {
  val cityPattern = """/deals/([a-zA-Z-]+)""".r

  val dealPattern = """/deals/[a-zA-Z]+/.*/\d+""".r

  val percentPattern = """[0-9]+%""".r

  def extractCityLinks(content: String) = {
    val parser = htmlParser(content)
    (for {
      span <- parser \\ "span"
      cssClass <- span \ "@class" if cssClass.text == "citiesSelectItem"
      onClick <- span \ "@onclick"
    } yield (span.text, extractCityUrl(onClick.text).getOrElse("none"))).toSet
  }

  def extractCityUrl(content: String) =
    cityPattern.findFirstMatchIn(content).map(_.group(1))

  def extractDealLinks(content: String) = {
    dealPattern.findAllIn(content).toSet
  }

  // <meta name="description" content="Mongolisches All-you-can-eat-Buffet mit ..."/>
  def extractDealData(content: String) = {
    println("startparse", DateTime.now().getMillisOfDay)
    val start = System.currentTimeMillis()
    val parser = htmlParser(content)

    // get desc
    val metatags = parser \ "head" \ "meta"
    val metaDescTag = metatags
      .filter(_.attribute("name").isDefined)
      .filter(_.attribute("name").get.text == "description")
    val desc = metaDescTag
      .flatMap(_.attribute("content"))
      .map(_.text).headOption
      .getOrElse("no desc")

    val divs = parser \\ "div"
    val savingsDiv = divs.find {
      div =>
        (div \ "@class").text == "savings2"
    }
    val spans = savingsDiv.map(_ \ "span").filter(!_.isEmpty)
    val percent = spans.flatMap {
      span =>
        percentPattern
          .findFirstIn(span.text)
          .map(_.reverse.drop(1).reverse)
          .map("." + _ + (math.random * 100).toInt.toString)
    }

    println("time for parsing: ", System.currentTimeMillis() - start)

    percent.map(percent => (desc, percent))
  }

  private def htmlParser(content: String) = {
    val reader = new StringReader(content)
    val parserFactory = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
    val parser = parserFactory.newSAXParser()
    val source = new org.xml.sax.InputSource(reader)
    val adapter = new scala.xml.parsing.NoBindingFactoryAdapter
    adapter.loadXML(source, parser)
  }
}
