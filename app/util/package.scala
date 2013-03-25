package object util {

  def asTsv(list: Traversable[(String,String)]) = {
    val header = "desc\tdiscount\tlongdesc\n"
    list.map{ case (desc,percent) => desc.take(7) + "\t" + percent + "\t" + desc}
      .mkString(header, "\n", "")
  }

  def gpnLink(path : String) = "http://www.groupon.de" + path
}
