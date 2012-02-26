organization := "com.bimbr"

name := "clisson-server"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

crossPaths := false

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies ++= Seq(
  "com.bimbr"                 % "clisson-protocol" % "0.1.0-SNAPSHOT",
  "com.typesafe"             %% "play-mini"        % "2.0-RC3-SNAPSHOT",
  "com.typesafe.akka"         % "akka-actor"       % "2.0-RC2",
  "org.mortbay.jetty"         % "jetty"            % "6.1.25",
  "org.specs2"               %% "specs2"           % "1.8.2"           % "test"  
)
