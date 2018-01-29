name := "Theia"

version := "0.1"

scalaVersion := "2.12.4"

assemblyJarName in assembly := "theia.jar"
mainClass in assembly := Some("org.red.theia.Server")

val meta = """.*(RSA|DSA)$""".r

// Hax to get .jar to execute
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) =>
    xs.map(_.toLowerCase) match {
      case ("manifest.mf" :: Nil) |
           ("index.list" :: Nil) |
           ("dependencies" :: Nil) |
           ("bckey.dsa" :: Nil) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  case PathList("reference.conf") | PathList("application.conf") => MergeStrategy.concat
  case PathList(_*) => MergeStrategy.first
}

scalacOptions ++= Seq("-deprecation", "-feature")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


resolvers ++=
  Seq("Artifactory Realm" at "http://maven.red.greg2010.me/artifactory/sbt-local/")

val slickVersion = "3.2.1"
val circeVersion = "0.9.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "de.heikoseeberger" %% "akka-http-circe" % "1.19.0",
  "com.typesafe" % "config" % "1.3.1",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
//  "com.github.pukkaone" % "logback-gelf" % "1.1.10",
  "org.quartz-scheduler" % "quartz" % "2.3.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.softwaremill.sttp" %% "core" % "1.1.4",
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.1.4",
  "org.red" %% "eve-sde-slick" % "0.2-SNAPSHOT",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.github.tminglei" %% "slick-pg" % "0.15.0",
  "org.scala-graph" %% "graph-core" % "1.12.1",
  "org.postgresql" % "postgresql" % "42.2.1")

slick := { slickCodeGenTask.value } // register manual sbt command
//sourceGenerators in Compile <+= slickCodeGenTask // register automatic code generation on every compile, remove for only manual use


// code generation task
lazy val slick = TaskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = Def.task {
  val dir = sourceDirectory.value / "main" / "scala"
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  val outputDir = dir.getPath // place generated files in sbt's managed sources folder
  r.run("org.red.SlickCodegen.CustomCodeGen", cp.files, Array(outputDir), s.log).failed foreach(sys error _.getMessage)
  val fname = outputDir
  Seq(file(fname))
}