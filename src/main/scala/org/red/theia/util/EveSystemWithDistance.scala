package org.red.theia.util

case class EveSystemWithDistance(id: Int,
                                 name: String,
                                 position: Position,
                                 securityStatus: Double,
                                 neighbours: List[Int],
                                 distance: Int) {
  def toEveSystem: EveSystem = {
    EveSystem(
      id = id,
      name = name,
      position = position,
      securityStatus = securityStatus,
      neighbours = neighbours
    )
  }
}