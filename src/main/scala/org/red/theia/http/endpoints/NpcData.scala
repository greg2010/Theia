package org.red.theia.http.endpoints

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Printer
import io.circe.generic.auto._
import org.red.theia.controllers.{NpcKillDataController, UniverseController}
import org.red.theia.http.{ConstellationDeltaKillsDataResponse, DataResponse, EveSystemResponse, SystemDeltaKillsDataResponse}
import org.red.theia.http.exceptions.{AmbiguousException, BadArgumentException, ResourceNotFoundException}
import org.red.theia.util.{EveSystem, EveSystemWithDistance, SystemDeltaKillsData}
import org.red.theia.Implicits._

import scala.concurrent.{ExecutionContext, Future}

trait NpcData
  extends LazyLogging
    with FailFastCirceSupport {


  def getSystems(name: String, distance: Int)
                (universeController: UniverseController, npcKillDataController: NpcKillDataController): (EveSystem, List[EveSystemWithDistance]) = {
    if (distance > 100) throw BadArgumentException("distance")
    universeController.getSystemsByName(name) match {
      case List() => throw ResourceNotFoundException("systemName")
      case List(head) => (head, universeController.getWithinNJumps(head, distance))
      case head :: tail => throw AmbiguousException("systemName", head.name :: tail.map(_.name))
    }
  }

  def npcDataEndpoints(universeController: => UniverseController,
                   npcKillDataController: => NpcKillDataController)
                   (implicit ec: ExecutionContext, printer: Printer): Route = pathPrefix("npcData") {
    pathPrefix(Segment) { systemName =>
      pathPrefix("constellationMedians") {
        (get & parameters("kills".?(true), "delta".?(true), "distance".? (10))) { (kills, delta, distance) =>
          complete {
            val (origin, withinRange) = getSystems(systemName, distance)(universeController, npcKillDataController)
            universeController.getConstellationsBySystems(origin, withinRange).flatMap { constellations =>
              (kills, delta) match {
                case (true, true) => npcKillDataController.getConstellationMedianDeltaKills(constellations)
                case (true, false) => npcKillDataController.getConstellationMedianKills(constellations)
                case (false, true) => npcKillDataController.getConstellationMedianDeltas(constellations)
                case _ => throw BadArgumentException("kills=false, delta=false")
              }
            }.map { d =>
              DataResponse(d.map { data =>
                ConstellationDeltaKillsDataResponse(
                  data.constellation.id,
                  data.constellation.name,
                  EveSystemResponse.fromEveSystemWithDistance(data.constellation.closestSystem),
                  data.npcMedianDelta,
                  data.npcMedianKills,
                  data.lastUpdated)
              })
            }
          }
        }
      } ~
        (get & parameters("kills".?(true), "delta".?(true), "distance".? (10))) { (kills, delta, distance) =>
          complete {
            val (origin, withinRange) = getSystems(systemName, distance)(universeController, npcKillDataController)
            val data = (kills, delta) match {
              case (true, true) => npcKillDataController.getSystemDeltaAndKills(withinRange)
              case (true, false) => npcKillDataController.getSystemKills(withinRange)
              case (false, true) => npcKillDataController.getSystemDeltas(withinRange)
              case _ => throw BadArgumentException("kills=false, delta=false")
            }

            data.map { d =>
              DataResponse(d.map { data =>
                SystemDeltaKillsDataResponse(data.system.id, data.system.name, data.system.distance, data.npcDelta, data.npcKills, data.lastUpdated)
              })
            }
          }
        }
    }
  }
}
