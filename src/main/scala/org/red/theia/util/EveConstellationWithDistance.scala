package org.red.theia.util

case class EveConstellationWithDistance(id: Int, name: String, systems: List[EveSystemWithDistance]) {
  override def toString: String = name.replace(" ", "_")
  val closestSystem: EveSystemWithDistance = systems.minBy(_.distance)
  val distance: Int = closestSystem.distance
}