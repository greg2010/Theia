package org.red.theia

import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import org.red.theia.controllers.{NpcKillDataController, ScheduleController, UniverseController}
import org.red.theia.http.endpoints.Base
import org.red.theia.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn


object Server extends App with LazyLogging with Base {

  val universeController = new UniverseController
  val npcKillDataController = new NpcKillDataController
  val scheduleController = new ScheduleController(npcKillDataController)

  val route = this.baseRoute(universeController, npcKillDataController)
  val server = Http().bindAndHandle(route, theiaConfig.getString("host"), theiaConfig.getInt("port"))
  logger.info(s"Server online at http://${theiaConfig.getString("host")}:${theiaConfig.getInt("port")}/\n" +
    s"Press RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  server
    .onComplete(_ => system.terminate()) // and shutdown when done
}
