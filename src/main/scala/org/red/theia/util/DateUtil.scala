package org.red.theia.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import scala.concurrent.duration._

object DateUtil {
  val xmlDateTimeFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz")
  val esiDateTimeFormat: SimpleDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz")
  val isoDateTimeFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
  private def fromDateTime(dt: String, fmt: SimpleDateFormat): Timestamp = new Timestamp(fmt.parse(dt).getTime)
  def fromXMLDateTime(dt: String): (Timestamp, Timestamp) = {
    val to = xmlDateTimeFormat.parse(dt + " GMT").getTime
    val from = to - 1.hour.toMillis
    (new Timestamp(from), new Timestamp(to))
  }

  def fromIsoDateTime(ts: String): Timestamp = fromDateTime(ts, isoDateTimeFormat)
  def fromEsiDateTime(ts: String): Timestamp = fromDateTime(ts, esiDateTimeFormat)
  def fromEsiDateTime(from: String, to: String): (Timestamp, Timestamp) = {
    (fromDateTime(from, esiDateTimeFormat), fromDateTime(to, esiDateTimeFormat))
  }
}
