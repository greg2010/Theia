package org.red.theia.http

import java.sql.Timestamp

import org.red.theia.util.EveSystemWithDistance

case class DataResponse[T](data: T)
case class ErrorResponse(reason: String, code: Int = 1)

case class AmbiguousResponse(value: String, options: Seq[String])

case class EveSystemResponse(systemId: Int, SystemName: String, distance: Int)

object EveSystemResponse {
  def fromEveSystemWithDistance(eveSystemWithDistance: EveSystemWithDistance): EveSystemResponse = {
    EveSystemResponse(eveSystemWithDistance.id, eveSystemWithDistance.name, eveSystemWithDistance.distance)
  }
}

case class SystemDeltaKillsDataResponse(systemId: Int, systemName: String, distance: Int, npcDelta: Option[Int], npcKills: Option[Int], lastUpdated: Option[Timestamp])

case class ConstellationDeltaKillsDataResponse(constellationId: Int,
                                               constellationName: String,
                                               closestSystem: EveSystemResponse,
                                               npcMedianDelta: Option[Int],
                                               npcMedianKills: Option[Int],
                                               lastUpdated: Option[Timestamp])