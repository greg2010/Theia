package org.red.theia.http.exceptions

case class AmbiguousException(ambiguousValue: String, valueOptions: List[String]) extends RuntimeException