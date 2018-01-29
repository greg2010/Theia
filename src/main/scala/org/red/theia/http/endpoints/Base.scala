package org.red.theia.http.endpoints

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import org.red.theia.theiaConfig
import org.red.theia.Implicits._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Printer
import org.red.theia.controllers.{NpcKillDataController, UniverseController}
import org.red.theia.http.{ApacheLog, Middleware}

import scala.concurrent.ExecutionContext


trait Base
  extends ApacheLog
    with Middleware
    with LazyLogging
    with FailFastCirceSupport
    with NpcData {

  def baseRoute(universeController: => UniverseController,
                npcKillDataController: => NpcKillDataController)(implicit ec: ExecutionContext, printer: Printer): Route = {
    val handleErrors = handleExceptions(exceptionHandler)
    accessLog(logger)(system.dispatcher, timeout, materializer) {
      handleErrors {
        pathPrefix(theiaConfig.getString("basePath")) {
          npcDataEndpoints(universeController, npcKillDataController) ~
          get {
            complete(StatusCodes.OK)
          }
        }
      }
    }
  }
}
