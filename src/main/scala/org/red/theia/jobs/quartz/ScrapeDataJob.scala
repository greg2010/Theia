package org.red.theia.jobs.quartz

import com.typesafe.scalalogging.LazyLogging
import org.quartz.{Job, JobExecutionContext}
import org.red.db.models.Theia
import org.red.theia.controllers.{NpcKillDataController, ScheduleController}
import org.red.theia.theiaDbObject
import slick.jdbc.PostgresProfile.api._
import org.red.theia.util.DATASOURCE
import org.red.theia.util.DATASOURCE.{ESI, XML}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


class ScrapeDataJob extends Job with LazyLogging {
  override def execute(context: JobExecutionContext): Unit = {

    implicit val ec: ExecutionContext = context.getScheduler.getContext.get("ec").asInstanceOf[ExecutionContext]
    val npcKillDataController = context.getScheduler.getContext.get("npcKillDataController").asInstanceOf[NpcKillDataController]
    val scheduleController = context.getScheduler.getContext.get("scheduleController").asInstanceOf[ScheduleController]
    val jobType = context.getMergedJobDataMap.getString("jobType")
    val resp = DATASOURCE.fromString(jobType) match {
      case XML => npcKillDataController.fetchLegacyApiData
      case ESI => npcKillDataController.fetchESIApiData
    }
    resp.map { r =>
      val insertRows = r.systemData.map { dr =>
        // row id is SERIAL in postgres, which means that whatever value is supplied will me disregarded. Hence `42`.
        Theia.NpcKillDataRow(42, dr.systemId, dr.npcKills, dr.from, dr.to, dr.source.toString)
      }
      val q = Theia.NpcKillData ++= insertRows
      val f = theiaDbObject.run(q)
      f.onComplete {
        case Success(res) => logger.info(s"Insert successful, datasource=$jobType rowsAffected=${res.getOrElse(0)}")
        case Failure(ex) => logger.error(s"Failure to insert rows datasource=$jobType", ex)
      }
      scheduleController.scheduleJob(jobType, r.cachedUntil)
    }
  }
}
