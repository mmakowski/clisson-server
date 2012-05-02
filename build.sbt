organization := "com.bimbr"

name := "clisson-server"

version := "0.2.0-SNAPSHOT"

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
  "com.typesafe.akka"          % "akka-actor"            % "2.0",
  "commons-configuration"      % "commons-configuration" % "1.8",
  "org.mashupbots.socko"      %% "socko-webserver"       % "0.1.0",
  "com.bimbr"                  % "clisson-client"        % "0.1.2"          % "test",  
  "junit"                      % "junit"                 % "4.10"           % "test", 
  "log4j"                      % "log4j"                 % "1.2.16"         % "test",
  "org.apache.httpcomponents"  % "httpclient"            % "4.1.3"          % "test",
  "org.slf4j"                  % "slf4j-api"             % "1.6.4"          % "test",
  "org.specs2"                %% "specs2"                % "1.8.2"          % "test"
)

scalacOptions += "-deprecation"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

mainClass in oneJar := Some("com.bimbr.clisson.server.ClissonServerApp")
