package org.red.theia.http.exceptions

case class AmbiguousException(value: String, options: List[String]) extends RuntimeException