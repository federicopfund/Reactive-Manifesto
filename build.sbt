name := "web"

organization := "vortex"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.13.12"

// Asset pipeline: digest (fingerprinting) → gzip (compression)
// Ensures browsers always load fresh CSS/JS after each deploy
pipelineStages := Seq(digest, gzip)

libraryDependencies ++= Seq(
  guice,
  jdbc, // Play JDBC API

  // Reactive Manifesto - Core message-driven
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",

  // Database - Slick (Reactive ORM)
  "org.playframework" %% "play-slick" % "6.1.1",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",
  
  // H2 Database (in-memory for development)
  "com.h2database" % "h2" % "2.2.224",
  
  // PostgreSQL Driver
  "org.postgresql" % "postgresql" % "42.7.1",

  // BCrypt for password hashing
  "org.mindrot" % "jbcrypt" % "0.4",

  // Email sending
  "com.sun.mail" % "javax.mail" % "1.6.2",

  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
)

// Seguridad ante conflictos transitivos
ThisBuild / evictionErrorLevel := Level.Warn
