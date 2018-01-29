package org.red.theia.controllers


import java.io.FileOutputStream
import java.io.ObjectOutputStream

import com.typesafe.scalalogging.LazyLogging
import org.red.db.models.Sde
import org.red.theia.sdeDbObject
import org.red.theia.util.{EveSystem, Position}
import slick.jdbc.PostgresProfile.api._
import java.io.FileInputStream
import java.io.ObjectInputStream

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scalax.collection.GraphEdge.UnDiEdge
import scalax.collection.immutable.Graph

class UniverseController(implicit ec: ExecutionContext) extends LazyLogging {
  private val graphFileName = "bin/eveUniverseGraph.bin"

  val systemData: List[EveSystem] = {
    val t0 = System.currentTimeMillis()
    val q1 = Sde.Mapsolarsystems.map(r => (r.solarsystemid, r.solarsystemname, r.x, r.y, r.z, r.security))
    def q2(systemId: Int) = Sde.Mapsolarsystemjumps.filter(_.fromsolarsystemid === systemId).map(_.tosolarsystemid)
    val f = for {
      solarSystemList <- sdeDbObject.run(q1.result)
      eveSystemList <- Future.sequence {
        solarSystemList.map { ss =>
          sdeDbObject.run(q2(ss._1).result).map { neighbours =>
            EveSystem(ss._1, ss._2.get, Position(ss._3.get, ss._4.get, ss._5.get), ss._6.get, neighbours.toList)
          }
        }
      }
    } yield eveSystemList.toList.filter(_.neighbours.nonEmpty)
    val res = Await.result(f, Duration.Inf)
    val t1 = System.currentTimeMillis()
    logger.info(s"Universe information fetched, time taken ${t1 - t0}ms")
    res
  }

  val eveUniverseGraph: Graph[EveSystem, UnDiEdge] = {
    val t0 = System.currentTimeMillis()
    val g = Try(readGraph) match {
      case Success(gr) => gr
      case Failure(ex) =>
        logger.warn("Failed to read graph from disk, regenerating graph", ex)
        val gr =  generateGraph(systemData)
        Future(writeGraph(gr)).onComplete {
          case Success(_) => logger.info("Graph generated and written to disk")
          case Failure(ex) => logger.error("Failed to write graph to disk", ex)
        }
        gr
    }
    val t1 = System.currentTimeMillis()
    logger.info(s"Graph built time taken ${t1 - t0}ms")
    g
  }

  private def writeGraph(g: Graph[EveSystem, UnDiEdge]): Unit = {
    val fos = new FileOutputStream(graphFileName)
    val out = new ObjectOutputStream(fos)
    out.writeObject(g)
    out.close()
  }

  private def readGraph: Graph[EveSystem, UnDiEdge] = {
    val fis = new FileInputStream(graphFileName)
    val in = new ObjectInputStream(fis)
    val g = in.readObject().asInstanceOf[Graph[EveSystem, UnDiEdge]]
    in.close()
    g
  }

  private def generateGraph(data: List[EveSystem]): Graph[EveSystem, UnDiEdge] = {
    systemData.foldRight(Graph[EveSystem, UnDiEdge]()){ (system, graphSoFar) =>
      system.neighbours.foldRight(graphSoFar)((neighbourId, systemGraphSoFar) =>
        systemData.find(_.id == neighbourId) match {
          case Some(neighbourSystem) => systemGraphSoFar + UnDiEdge[EveSystem](system, neighbourSystem)
          case None =>
            logger.error(s"Invalid neighbour with id $neighbourId")
            systemGraphSoFar
        }
      )
    }
  }

  private def getNode(outer: EveSystem): eveUniverseGraph.NodeT = eveUniverseGraph get outer

  def getSystemsByName(name: String): List[EveSystem] = {
    val regex = "^.*" + name.toUpperCase() + ".*$"
    systemData.filter(_.name.matches(regex))
  }

  def getWithinNJumps(system: EveSystem, n: Int): List[EveSystem] = getNode(system).withMaxDepth(n).map(_.toOuter).toList
}
