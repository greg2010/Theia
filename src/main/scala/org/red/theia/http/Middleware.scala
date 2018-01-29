package org.red.theia.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._

import scala.language.implicitConversions

trait Middleware extends LazyLogging with FailFastCirceSupport {
  implicit def ErrorResp2ResponseEntity(error: ErrorResponse): ResponseEntity = {
    HttpEntity(ContentType(MediaTypes.`application/json`), error.asJson.toString)
  }

  implicit def exceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      case exc: RuntimeException =>
        logger.error(s"Runtime exception caught, msg=${exc.getMessage}", exc)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = ErrorResponse("Internal Server Error")))
    }
  }
}
