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


ntriplesWriter.asString(timbl.graph,"")

jsonldCompactedWriter.asString(timbl.graph,"").get

val knows = timbl/foaf.knows
knows.map(_.pointer)

interp.load.ivy("org.scalaj" %% "scalaj-http" % "2.3.0")

@
import scalaj.http._
val bblUrl = URI("https://bblfish.net/people/henry/card#me")

bblUrl.fragmentLess
val bblReq = Http( bblUrl.fragmentLess.toString )


val bblDoc = bblReq.asString

val bg = turtleReader.read(new java.io.StringReader(bblDoc.body), bblUrl.fragmentLess.toString)

val pg = PointedGraph[Sesame](bblUrl,bg.get)
(pg/foaf.name).map(_.pointer)

val knows = pg/foaf.knows

knows.size

(knows/foaf.name).map(_.pointer)

import scala.util.{Try,Success,Failure}

case class IOException(docURL: Sesame#URI, e: java.lang.Throwable ) extends java.lang.Exception
def fetch(docUrl: Sesame#URI): Try[HttpResponse[scala.util.Try[Sesame#Graph]]] = {
  assert (docUrl == docUrl.fragmentLess)
  Try( //we want to catch connection exceptions
   Http(docUrl.toString)
    .header("Accept", "application/rdf+xml,text/turtle,application/ld+json,text/n3;q=0.2")
    .exec { case (code, headers, is) =>
      headers.get("Content-Type")
             .flatMap(_.headOption)
             .fold[Try[(io.RDFReader[Sesame, Try, _],String)]](
                   Failure(new java.lang.Exception("Missing Content Type"))
              ) { ct =>
        val ctype = ct.split(';')
        val parser = ctype(0).trim match {
          case "text/turtle" => Success(turtleReader)
          case "application/rdf+xml" => Success(rdfXMLReader)
          case "application/n-triples" => Success(ntriplesReader)
          case "application/ld+json" => Success(jsonldReader)
          case ct => Failure(new java.lang.Exception("Missing parser for "+ct))
        }
        parser.map{ p =>
             val attributes = ctype.toList.tail.flatMap(
             _.split(',').map(_.split('=').toList.map(_.trim))
          ).map(avl => (avl.head, avl.tail.headOption.getOrElse(""))).toMap
          val encoding = attributes.get("encoding").getOrElse("utf-8")
          (p, encoding)
        }
       } flatMap { case (parser,encoding) =>
        parser.read(new java.io.InputStreamReader(is, encoding), docUrl.toString)
      }
    }).recoverWith{case t => Failure(IOException(docUrl,t))}
}

val bblgrph = fetch(bblUrl.fragmentLess)
val bblfish: PointedGraph[Sesame] = PointedGraph[Sesame](bblUrl,bblgrph.get.body.get)
val bblFriends: PointedGraphs[Sesame] = bblfish/foaf.knows
val friendIds: Iterable[Sesame#Node] = bblFriends.map(_.pointer)

friendIds.size
val nodeTypes = friendIds.groupBy{ (u: Sesame#Node) => nodeW(u).fold[String](uri =>"uri", bnode=>"bnode", lit=>"literal") }

nodeTypes("uri").size

nodeTypes("bnode").size

val groupIds =  bblfish/-foaf.member | ( _.pointer )
(bblfish/foaf.holdsAccount | ( _.pointer )).size
