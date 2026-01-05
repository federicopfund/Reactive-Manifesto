name := "web"

organization := "vortex"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,

  // Reactive Manifesto - Core message-driven
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",

  // Database - Slick (Reactive ORM)
  "org.playframework" %% "play-slick" % "6.1.1",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",
  
  // H2 Database (in-memory for development)
  "com.h2database" % "h2" % "2.2.224",

  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)

// Seguridad ante conflictos transitivos
ThisBuild / evictionErrorLevel := Level.Warn
