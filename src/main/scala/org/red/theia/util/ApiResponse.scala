package org.red.theia.util

import java.sql.Timestamp

case class ApiResponse(systemData: Seq[SystemData], cachedUntil: Timestamp)
