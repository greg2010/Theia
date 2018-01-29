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
import scala.util.{Failure, Success}
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

  private def getDbDataForSystems(systems: List[EveSystem]): Future[Seq[Theia.NpcKillDataRow]] = {
    val q = Theia.NpcKillData.filter(row => row.systemId inSet systems.map(_.id.toLong))
    val f = theiaDbObject.run(q.result)
    f.onComplete {
      case Success(resp) => logger.info(s"DB request for system list successful response length=${resp.length}")
      case Failure(ex) => logger.error("Failed to get data from the DB", ex)
    }
    f
  }

  private def parseSystemDeltas(systems: List[EveSystem], dbData: Seq[Theia.NpcKillDataRow]): List[SystemDeltaKillsData] = {
    val twoHInThePast = new Timestamp(System.currentTimeMillis() - 2.hours.toMillis)
    val result = dbData.filter(_.fromTstamp.after(twoHInThePast)).groupBy(_.systemId)
      .map { systemData =>
        // Guaranteed to get since systemData only contains subset of systems
        val system = systems.find(_.id == systemData._1).get
        val npcDelta = systemData._2.length match {
          case 1 => systemData._2.head.npcKills // Only 1 record is present in the database
          case _ => // 2 or more records are present in the database
            val d = systemData._2.sortBy(_.fromTstamp.getTime).take(2)
            d.last.npcKills - d.head.npcKills
        }
        SystemDeltaKillsData(system, Some(npcDelta.toInt), None)
      }.toList

    result ++ systems.diff(result.map(_.system)).map(s => SystemDeltaKillsData(s, Some(0), None)).sortBy(_.system.id)
  }

  private def parseSystemKills(systems: List[EveSystem], dbData: Seq[Theia.NpcKillDataRow]): List[SystemDeltaKillsData] = {
    val twoHInThePast = new Timestamp(System.currentTimeMillis() - 2.hours.toMillis)
    val result = dbData.filter(_.fromTstamp.after(twoHInThePast)).groupBy(_.systemId)
      .map { systemData =>
        // Guaranteed to get since systemData only contains subset of systems
        val system = systems.find(_.id == systemData._1).get
        val npcKills = systemData._2.head.npcKills
        SystemDeltaKillsData(system, None, Some(npcKills.toInt))
      }.toList

    result ++ systems.diff(result.map(_.system)).map(s => SystemDeltaKillsData(s, None, Some(0))).sortBy(_.system.id)
  }

  def getSystemDeltas(systems: List[EveSystem]): Future[List[SystemDeltaKillsData]] = {
    this.getDbDataForSystems(systems).map(resp => parseSystemDeltas(systems, resp).sortBy(_.deltaData.map(- _)))
  }

  def getSystemKills(systems: List[EveSystem]): Future[List[SystemDeltaKillsData]] = {
    this.getDbDataForSystems(systems).map(resp => parseSystemKills(systems, resp).sortBy(_.killsData.map(- _)))
  }

  def getSystemDeltaAndKills(systems: List[EveSystem]): Future[List[SystemDeltaKillsData]] = {
    for {
      dbData <- this.getDbDataForSystems(systems)
      deltaData <- Future(parseSystemDeltas(systems, dbData))
      killsData <- Future(parseSystemKills(systems, dbData))
    } yield deltaData.zip(killsData).map(x => SystemDeltaKillsData(x._1.system, x._1.deltaData, x._2.killsData)).sortBy(_.killsData.map(- _))
  }
}
