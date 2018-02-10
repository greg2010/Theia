package org.red.theia.controllers

import java.sql.Timestamp

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.typesafe.scalalogging.LazyLogging
import org.red.theia.util._
import io.circe.generic.auto._
import io.circe.parser
import org.red.db.models.Theia
import org.red.theia.util.esi.EsiSystemKillsData
import slick.jdbc.PostgresProfile.api._
import org.red.theia.{theiaDbObject, util}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class NpcKillDataController(implicit ec: ExecutionContext) extends LazyLogging {

  def fetchESIApiData: Future[ApiResponse] = {
    val query = "https://esi.tech.ccp.is/latest/universe/system_kills/?datasource=tranquility"
    implicit val backend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()
    val request = sttp.get(uri"$query")
    request.send().map { response =>
      val (from, to) = (response.header("last-modified"), response.header("expires")) match {
        case (Some(lm), Some(exp)) => DateUtil.fromEsiDateTime(lm, exp)
        case _ => throw new RuntimeException("Bad time")
      }

      val res = parser.decode[Seq[EsiSystemKillsData]](response.unsafeBody) match {
        case Right(data) =>
          data.map(esiData => SystemData(esiData.system_id, esiData.npc_kills, from, to, DATASOURCE.ESI))
        case Left(ex) => throw ex
      }
      ApiResponse(res, to)
    }
  }

  def fetchLegacyApiData: Future[ApiResponse] = {
    val query = "https://api.eveonline.com/map/kills.xml.aspx"
    implicit val backend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()
    val request = sttp.get(uri"$query")
    request.send().map { response =>
      val xmlData = XML.loadString(response.unsafeBody)

      val (from, to) = (xmlData \ "cachedUntil").headOption
        .map(dataTime => DateUtil.fromXMLDateTime(dataTime.text)) match {
        case Some(t) => t
        case None => throw new RuntimeException("Bad cachedUntil")
      }

      val res = (xmlData \ "result" \ "rowset" \ "row").map { row =>
        (row.attribute("solarSystemID").map(_.text), row.attribute("factionKills").map(_.text)) match {
          case (Some(systemId), Some(npcKills)) =>
            SystemData(systemId.toLong, npcKills.toInt, from, to, DATASOURCE.XML)
          case _ => throw new RuntimeException("Bad data")
        }
      }
      ApiResponse(res, to)
    }
  }

  private def getMedian(l: List[Int]): Int = {
    l.sortWith(_ < _).drop(l.length/2).head
  }

  private def max(ts1: Timestamp, ts2: Timestamp): Timestamp = new Timestamp(math.max(ts1.getTime, ts2.getTime))

  private def max(ts1: Option[Timestamp], ts2: Option[Timestamp]): Option[Timestamp] = {
    (ts1, ts2) match {
      case (Some(t1), Some(t2)) => Some(max(t1, t2))
      case (Some(t1), None) => Some(t1)
      case (None, Some(t2)) => Some(t2)
      case _ => None
    }
  }

  private def getDbDataForSystems(systems: List[EveSystemWithDistance]): Future[Seq[Theia.NpcKillDataRow]] = {
    val q = Theia.NpcKillData.filter(row => row.systemId inSet systems.map(_.id.toLong)).take(20)
    val f = theiaDbObject.run(q.result)
    f.onComplete {
      case Success(resp) => logger.info(s"DB request for system list successful response length=${resp.length}")
      case Failure(ex) => logger.error("Failed to get data from the DB", ex)
    }
    f
  }

  private def parseSystemDeltas(systems: List[EveSystemWithDistance], dbData: Seq[Theia.NpcKillDataRow]): List[SystemDeltaKillsData] = {
    val twoHInThePast = new Timestamp(System.currentTimeMillis() - 2.hours.toMillis)
    val result = dbData.filter(_.fromTstamp.after(twoHInThePast)).groupBy(_.systemId)
      .map { systemData =>
        // Guaranteed to get since systemData only contains subset of systems
        val system = systems.find(_.id == systemData._1).get
        val (npcDelta, timestamp) = systemData._2.length match {
          case 1 => (systemData._2.head.npcKills, systemData._2.head.fromTstamp) // Only 1 record is present in the database
          case _ => // 2 or more records are present in the database
            val d = systemData._2.sortBy(_.fromTstamp.getTime).take(2)
            val npcDelta = (d.last.fromTstamp.getTime - d.head.fromTstamp.getTime).millis.toMinutes match {
              case 0 => (d.last.npcKills - d.head.npcKills) / 1
              case x => (d.last.npcKills - d.head.npcKills) / x
            }
            (npcDelta, max(d.head.fromTstamp, d.last.fromTstamp))
        }
        SystemDeltaKillsData(system, Some(npcDelta.toInt), None, Some(timestamp))
      }.toList

    result ++ systems.diff(result.map(_.system)).map(s => SystemDeltaKillsData(s, Some(0), None, None)).sortBy(_.system.id)
  }

  private def parseSystemKills(systems: List[EveSystemWithDistance], dbData: Seq[Theia.NpcKillDataRow]): List[SystemDeltaKillsData] = {
    val twoHInThePast = new Timestamp(System.currentTimeMillis() - 2.hours.toMillis)
    val result = dbData.filter(_.fromTstamp.after(twoHInThePast)).groupBy(_.systemId)
      .map { systemData =>
        // Guaranteed to get since systemData only contains subset of systems
        val system = systems.find(_.id == systemData._1).get
        val npcKills = systemData._2.head.npcKills
        val lastUpdated = systemData._2.head.fromTstamp
        SystemDeltaKillsData(system, None, Some(npcKills.toInt), Some(lastUpdated))
      }.toList

    result ++ systems.diff(result.map(_.system)).map(s => SystemDeltaKillsData(s, None, Some(0), None)).sortBy(_.system.id)
  }

  def getSystemDeltas(systems: List[EveSystemWithDistance]): Future[List[SystemDeltaKillsData]] = {
    this.getDbDataForSystems(systems).map(resp => parseSystemDeltas(systems, resp).sortBy(_.npcDelta.map(- _)))
  }

  def getSystemKills(systems: List[EveSystemWithDistance]): Future[List[SystemDeltaKillsData]] = {
    this.getDbDataForSystems(systems).map(resp => parseSystemKills(systems, resp).sortBy(_.npcKills.map(- _)))
  }

  def getSystemDeltaAndKills(systems: List[EveSystemWithDistance]): Future[List[SystemDeltaKillsData]] = {
    for {
      dbData <- this.getDbDataForSystems(systems)
      deltaData <- Future(parseSystemDeltas(systems, dbData))
      killsData <- Future(parseSystemKills(systems, dbData))
    } yield deltaData.zip(killsData)
      .map(x => SystemDeltaKillsData(x._1.system, x._1.npcDelta, x._2.npcKills, max(x._1.lastUpdated, x._2.lastUpdated)))
      .sortBy(_.npcKills.map(- _))
  }

  def getConstellationMedianDeltas(constellations: List[EveConstellationWithDistance]): Future[List[ConstellationMedianKillsDeltaData]] = {
    Future.sequence {
      constellations.map { constellation =>
        getSystemDeltas(constellation.systems).map { systemDeltas =>
          val medianDelta = getMedian(systemDeltas.flatMap(_.npcDelta))
          val emptyTs: Option[Timestamp] = None
          val lastUpdated = systemDeltas.map(_.lastUpdated).foldRight(emptyTs)((l, acc) => max(l, acc))
          ConstellationMedianKillsDeltaData(constellation, Some(medianDelta), None, lastUpdated)
        }
      }
    }
  }.map(_.sortBy(_.npcMedianDelta.map(- _)))

  def getConstellationMedianKills(constellations: List[EveConstellationWithDistance]): Future[List[ConstellationMedianKillsDeltaData]] = {
    Future.sequence {
      constellations.map { constellation =>
        getSystemKills(constellation.systems).map { systemKills =>
          val medianKills = getMedian(systemKills.flatMap(_.npcKills))
          val emptyTs: Option[Timestamp] = None
          val lastUpdated = systemKills.map(_.lastUpdated).foldRight(emptyTs)((l, acc) => max(l, acc))
          ConstellationMedianKillsDeltaData(constellation, None, Some(medianKills), lastUpdated)
        }
      }
    }
  }.map(_.sortBy(_.npcMedianKills.map(- _)))

  def getConstellationMedianDeltaKills(constellations: List[EveConstellationWithDistance]): Future[List[ConstellationMedianKillsDeltaData]] = {
    Future.sequence {
      constellations.map { constellation =>
        getSystemDeltaAndKills(constellation.systems).map { systemDeltaKills =>
          val medianDelta = getMedian(systemDeltaKills.flatMap(_.npcDelta))
          val medianKills = getMedian(systemDeltaKills.flatMap(_.npcKills))
          val emptyTs: Option[Timestamp] = None
          val lastUpdated = systemDeltaKills.map(_.lastUpdated).foldRight(emptyTs)((l, acc) => max(l, acc))
          ConstellationMedianKillsDeltaData(constellation, Some(medianDelta), Some(medianKills), lastUpdated)
        }
      }
    }.map(_.sortBy(_.npcMedianKills.map(- _)))
  }
}
