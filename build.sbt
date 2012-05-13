organization := "com.bimbr"

name := "clisson-server"

version := "0.3.0-SNAPSHOT"

scalaVersion := "2.9.1"

crossPaths := false

resolvers ++= Seq(
  "Typesafe Repository"           at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback"             % "logback-classic"       % "1.0.2",
  "com.bimbr"                  % "clisson-protocol"      % "0.1.0",
  "com.h2database"             % "h2"                    % "1.3.158",
  "com.typesafe.akka"          % "akka-actor"            % "2.0.1",
  "commons-configuration"      % "commons-configuration" % "1.8",
  "org.mashupbots.socko"      %% "socko-webserver"       % "0.1.0",
  "com.bimbr"                  % "clisson-client"        % "0.3.0"          % "test",  
  "com.typesafe.akka"          % "akka-testkit"          % "2.0.1"          % "test",
  "junit"                      % "junit"                 % "4.10"           % "test", 
  "log4j"                      % "log4j"                 % "1.2.16"         % "test",
  "org.apache.httpcomponents"  % "httpclient"            % "4.1.3"          % "test",
  "org.slf4j"                  % "slf4j-api"             % "1.6.4"          % "test",
  "org.specs2"                %% "specs2"                % "1.8.2"          % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked")

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

mainClass in oneJar := Some("com.bimbr.clisson.server.ClissonServerApp")

filesToInclude in zip := Seq(
  "src/main/distributables/clisson-server"       -> "clisson-server",
  "src/main/distributables/clisson-server.cmd"   -> "clisson-server.cmd",
  "src/main/distributables/logback.xml"          -> "logback.xml",
  "src/main/static/favicon.ico"                  -> "static/favicon.ico",
  "src/test/resources/clisson-server.properties" -> "clisson-server.properties"
)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
