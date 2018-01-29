package org.red.theia.http

case class DataResponse[T](data: T)
case class ErrorResponse(reason: String, code: Int = 1)


case class SystemDeltaKillsDataResponse(systemId: Int, systemName: String, deltaData: Option[Int], killsData: Option[Int])

