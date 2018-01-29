package org.red.theia.util

import com.typesafe.scalalogging.LazyLogging
import math.{pow, sqrt}

case class Position(x: Double, y: Double, z: Double)

case class EveSystem(id: Int, name: String, position: Position, securityStatus: Double, neighbours: List[Int]) extends LazyLogging {
  override def toString: String = name.replace(" ", "_")

  def distance(other: EveSystem): Double = {
    val ccpLY: Double = 9460528450000000D
    val dist = sqrt(
      pow(this.position.x - other.position.x, 2) +
      pow(this.position.y - other.position.y, 2) +
      pow(this.position.z - other.position.z, 2)
    ) / ccpLY
    //logger.debug(s"Distance between ${this.name} and ${other.name} is ${dist}LY")
    dist
  }

  def isJumpable: Boolean = securityStatus < 0.5
}
