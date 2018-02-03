package org.red.theia.util

import java.sql.Timestamp

case class ConstellationMedianKillsDeltaData(constellation: EveConstellationWithDistance,
                                             npcMedianDelta: Option[Int],
                                             npcMedianKills: Option[Int],
                                             lastUpdated: Option[Timestamp])
