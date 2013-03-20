import scala.util.matching.Regex
import java.io.StringReader

/**
 *
 */
package object util {
  val pattern: Regex = """/deals/[a-zA-Z]+/.*/\d+""".r

  def getDealLinks(content: String) = {
    pattern.findAllIn(content).toList
  }

  // <meta name="description" content="Mongolisches All-you-can-eat-Buffet mit ..."/>
  def data(content: String) = {
    val parser = source(content)
    val metatags = parser \ "head" \ "meta"
    val metaDescTag = metatags
      .filter(_.attribute("name").isDefined)
      .filter(_.attribute("name").get.text == "description")
    metaDescTag.flatMap(_.attribute("content")).map(_.text).headOption
  }

  def source(content: String) = {
    val reader = new StringReader(content)
    val parserFactory = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
    val parser = parserFactory.newSAXParser()
    val source = new org.xml.sax.InputSource(reader)
    val adapter = new scala.xml.parsing.NoBindingFactoryAdapter
    adapter.loadXML(source, parser)
  }
}
