package io.surfkit.derpyhoves.flows

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.joda.time.DateTimeZone
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * Created by suroot on 18/07/17.
  */
class TimeSeries(url: String, interval: FiniteDuration, hoursOpt: Option[DateTimeZone] = None)(implicit system: ActorSystem, materializer: Materializer) {
  import scala.concurrent.duration._
  val request: _root_.akka.http.scaladsl.model.HttpRequest = RequestBuilding.Get(Uri(url))
  val source: Source[HttpRequest, Cancellable] = Source.tick(0.seconds, interval, request).filter{ _ =>
    hoursOpt.map{ timezone =>
      val dt = new DateTime(timezone)
      dt.getHourOfDay >= 8 && dt.getHourOfDay <= 16 && dt.getDayOfWeek() >= org.joda.time.DateTimeConstants.MONDAY && dt.getDayOfWeek() <= org.joda.time.DateTimeConstants.FRIDAY
    }.getOrElse(true)
  }
  val sourceWithDest: Source[Try[HttpResponse], Cancellable] = source.map(req ⇒ (req, NotUsed)).via(Http().superPool[NotUsed]()).map(_._1)

  def apply(): Source[Try[HttpResponse], Cancellable] = sourceWithDest

  def shutdown = {
    Http().shutdownAllConnectionPools()
  }

}
