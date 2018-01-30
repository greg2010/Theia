package org.red.theia.controllers

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.duration._

import com.typesafe.scalalogging.LazyLogging
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.red.theia.jobs.quartz.ScrapeDataJob

import scala.concurrent.ExecutionContext

class ScheduleController(npcKillDataController: NpcKillDataController)(implicit ec: ExecutionContext) extends LazyLogging {
  val jobIdentity = "jobKickstart"
  val quartzScheduler: Scheduler = new StdSchedulerFactory().getScheduler
  quartzScheduler.getContext.put("ec", ec)
  quartzScheduler.getContext.put("scheduleController", this)
  quartzScheduler.getContext.put("npcKillDataController", npcKillDataController)
  quartzScheduler.start()

  kickstartJob("XML")
  kickstartJob("ESI")


  def kickstartJob(jobType: String): Date = {
    val now = new Timestamp(System.currentTimeMillis() + 1.second.toMillis)
    scheduleJob(jobType, now)
  }

  def scheduleJob(jobType: String, at: Timestamp): Date = {
    val genIdentity = jobIdentity + at.getTime + jobType
    val j = newJob((new ScrapeDataJob).getClass)
      .withIdentity(genIdentity)
      .build()
    j.getJobDataMap.put("jobType", jobType)
    val t = newTrigger()
      .withIdentity(genIdentity)
      .forJob(j)
      .startAt(new Date(at.getTime))
      .build()
    val resp = quartzScheduler.scheduleJob(j, t)
    logger.info(s"Scheduled new job jobType=$jobType date=${resp.toString}")
    resp
  }
}
