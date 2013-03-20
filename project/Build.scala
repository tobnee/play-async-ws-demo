import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "groupon"
    val appVersion      = "0.1"
	
    val appDependencies = Seq(
	"org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"
    ) 
 
    val main = play.Project(
      appName, appVersion, appDependencies
    ) 

}
