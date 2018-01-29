package org.red.theia.http.endpoints

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Printer
import io.circe.generic.auto._
import org.red.theia.controllers.{NpcKillDataController, UniverseController}
import org.red.theia.http.{DataResponse, SystemDeltaKillsDataResponse}
import org.red.theia.http.exceptions.{AmbiguousException, BadArgumentException}

import scala.concurrent.ExecutionContext

trait NpcData
  extends LazyLogging
    with FailFastCirceSupport {

  def npcDataEndpoints(universeController: => UniverseController,
                   npcKillDataController: => NpcKillDataController)
                   (implicit ec: ExecutionContext, printer: Printer): Route = pathPrefix("npcData") {
    pathPrefix(Segment) { systemName =>
      (get & parameters("kills".?(true), "delta".?(true), "distance".? (10))) { (kills, delta, distance) =>
        complete {
          if (distance > 100) throw BadArgumentException("distance")
          val data = universeController.getSystemsByName(systemName) match {
            case List() => throw BadArgumentException("systemName")
            case List(head) =>
              val withinRange = universeController.getWithinNJumps(head, distance)
              (kills, delta) match {
                case (true, true) => npcKillDataController.getSystemDeltaAndKills(withinRange)
                case (true, false) => npcKillDataController.getSystemKills(withinRange)
                case (false, true) => npcKillDataController.getSystemDeltas(withinRange)
                case _ => throw BadArgumentException("kills=false, delta=false")
              }
            case head :: tail => throw AmbiguousException("systemName", head.name :: tail.map(_.name))
          }
          data
            .map { d =>
              DataResponse(d.map { data =>
                SystemDeltaKillsDataResponse(data.system.id, data.system.name, data.deltaData, data.killsData)
              })
            }
        }
      }
    }
  }
}
