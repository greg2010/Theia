package org.red.theia.util

import java.sql.Timestamp

case class SystemDeltaKillsData(system: EveSystemWithDistance, npcDelta: Option[Int], npcKills: Option[Int], lastUpdated: Option[Timestamp])
