package org.red.theia.util

import java.sql.Timestamp


sealed trait DATASOURCE

object DATASOURCE {
  case object XML extends DATASOURCE {
    override def toString: String = "XML"
  }
  case object ESI extends DATASOURCE  {
    override def toString: String = "ESI"
  }
  def fromString(st: String): DATASOURCE = {
    st match {
      case "XML" => this.XML
      case "ESI" => this.ESI
      case _ => throw new RuntimeException("Bad DATASOURCE type")
    }
  }
}


case class SystemData(systemId: Long, npcKills: Int, from: Timestamp, to: Timestamp, source: DATASOURCE)