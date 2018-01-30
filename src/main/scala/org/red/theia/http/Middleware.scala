package org.red.theia.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import org.red.theia.http.exceptions.{AmbiguousException, BadArgumentException, ResourceNotFoundException}

import scala.language.implicitConversions

trait Middleware extends LazyLogging with FailFastCirceSupport {
  implicit def ErrorResp2ResponseEntity(error: ErrorResponse): ResponseEntity = {
    HttpEntity(ContentType(MediaTypes.`application/json`), error.asJson.toString)
  }
  implicit def AmbiguousResp2ResponseEntity(error: AmbiguousResponse): ResponseEntity = {
    HttpEntity(ContentType(MediaTypes.`application/json`), error.asJson.toString)
  }

  implicit def exceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      case exc: BadArgumentException =>
        logger.warn(s"ResourceNotFound exception caught, value=${exc.name}")
        complete(HttpResponse(StatusCodes.BadRequest, entity = ErrorResponse(s"Bad argument ${exc.name}")))
      case exc: ResourceNotFoundException =>
        logger.warn(s"ResourceNotFound exception caught, value=${exc.value}")
        complete(HttpResponse(StatusCodes.NotFound, entity = ErrorResponse(s"${exc.value} not found")))
      case exc: AmbiguousException =>
        logger.warn(s"Ambiguous exception caught, value=${exc.value} options=${exc.options.mkString(",")}")
        complete(HttpResponse(StatusCodes.MultipleChoices, entity = AmbiguousResponse(exc.value, exc.options)))
      case exc: RuntimeException =>
        logger.error(s"Runtime exception caught, msg=${exc.getMessage}", exc)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = ErrorResponse("Internal Server Error")))
    }
  }
}
