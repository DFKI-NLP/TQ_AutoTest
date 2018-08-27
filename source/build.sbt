import play.Project._

name := "qt21"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  "com.github.tototoshi" %% "scala-csv" % "1.3.4",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "org.webjars" %% "webjars-play" % "2.2.2",
  "org.webjars" % "bootstrap" % "2.3.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

playScalaSettings

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
