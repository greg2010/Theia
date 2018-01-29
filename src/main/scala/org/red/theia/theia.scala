package org.red

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Printer
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration._
import scala.language.postfixOps


package object theia {
  val config: Config = ConfigFactory.load()
  val theiaConfig: Config = config.getConfig("theia")

  object Implicits {
    implicit val system: ActorSystem = ActorSystem("theia", config.getConfig("akka"))
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = Timeout(2 seconds)
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
  }

  private val dbConfig: Config =  ConfigFactory.load(ConfigFactory.parseFile(new File("src/main/resources/reference.conf")))
  val theiaDbObject: JdbcBackend.Database = Database.forConfig("theia", dbConfig)
  val sdeDbObject: JdbcBackend.Database = Database.forConfig("sde", dbConfig)

}
