import coursierapi.MavenRepository
interp.repositories.update(
interp.repositories() ::: List(MavenRepository.of("http://bblfish.net/work/repo/snapshots/"))
)

@
import $ivy.`org.w3::banana-sesame:0.8.4-SNAPSHOT`
import $ivy.`ch.qos.logback:logback-classic:1.2.3`
import $ivy.`com.typesafe.scala-logging::scala-logging:3.9.2`
@
import org.w3.banana._
import org.w3.banana.syntax._
import org.w3.banana.sesame.Sesame
import Sesame._
import ops._
import com.typesafe.scalalogging.Logger

val logger = Logger("banana-examples")
val foaf = FOAFPrefix[Sesame]
val timbl: PointedGraph[Sesame] = (
     URI("https://www.w3.org/People/Berners-Lee/card#i")
        -- foaf.name ->- "Tim Berners-Lee".lang("en")
        -- foaf.plan ->- "Make the Web Great Again"
        -- foaf.knows ->- (bnode("vint") -- foaf.name ->- "Vint Cerf")
        -- foaf.knows ->- URI("http://bblfish.net/people/henry/card#me")
 )

val graphString = ntriplesWriter.asString(timbl.graph,"")
logger.debug(s"ntriples as string $graphString \n")

val turtleString = turtleWriter.asString(timbl.graph,"").get
logger.debug(s"turtle string $turtleString \n")

val jsonLDString = jsonldCompactedWriter.asString(timbl.graph,"").get
logger.debug(s"jsonldCompacted string $jsonLDString \n")

val knows = timbl/foaf.knows
knows.map(_.pointer)
