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
  "com.h2database"            % "h2"               % "1.3.158",
  "com.typesafe"             %% "play-mini"        % "2.0-RC3",
  "com.typesafe.akka"         % "akka-actor"       % "2.0-RC4",
  "org.specs2"               %% "specs2"           % "1.8.2"             % "test"  
)

seq(ProguardPlugin.proguardSettings: _*)

proguardOptions ++= Seq(
  keepMain("com.bimbr.clisson.server.ClissonServerApp"),
  "-dontshrink"
)

//proguardLibraryJars <++= (update) map (_.select(module = moduleFilter(name = "config")))