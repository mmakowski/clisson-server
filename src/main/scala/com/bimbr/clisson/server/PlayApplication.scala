package com.bimbr.clisson.server

import com.typesafe.play.mini.{ POST, GET, Path, Application }
import play.api.mvc.{ Action, AsyncResult }
import play.api.mvc.Results._
import play.api.libs.concurrent._
import play.api.data._
import play.api.data.Forms._
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{ ActorSystem, Props, Actor }

object PlayApplication extends Application {
  def route = {
    case GET(Path("/ping")) => Action {
      Ok("Pong @ " + System.currentTimeMillis)
    }
  }
}