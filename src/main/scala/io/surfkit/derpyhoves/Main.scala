package io.surfkit.derpyhoves

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import io.surfkit.derpyhoves.flows._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json

object Main extends App{

  val API_KEY = "PMGX9ASKF5L4PW7E"

  override def main(args: Array[String]) {

    val decider: Supervision.Decider = {
      case _ => Supervision.Resume
    }
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))
/*

    import scala.concurrent.duration._
    val request: _root_.akka.http.scaladsl.model.HttpRequest = RequestBuilding.Get(Uri("https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=MSFT&interval=1min&apikey=PMGX9ASKF5L4PW7E"))
    val source: Source[HttpRequest, Cancellable] = Source.tick(0.seconds, 20.seconds, request)
    val sourceWithDest: Source[Try[HttpResponse], Cancellable] =
      source.map(req ⇒ (req, NotUsed)).via(Http().superPool[NotUsed]()).map(_._1)

    sourceWithDest.runForeach(i => println(i))(materializer)*/

    //val msft = AlphaVantageTimeSeries("MSFT", AlphaVantage.Interval.`1min`)
    //msft.json.runForeach(i => i.foreach(x => x.foreach(println) ) )(materializer)
   /* try {
      val fx = AlphaVantage.FullTimeSeries.get("MSFT", AlphaVantage.Interval.daily)
      fx.foreach { x =>
        println(s"GOT IT: ${x.`Time Series`.series.length}")
      }
      Thread.currentThread.join()
    }catch{
      case t:Throwable =>
        t.printStackTrace()
    }*/


    try {
      val api = new AlphaVantageApi(API_KEY)
      /*println("calling EMA")
      val fx = AlphaVantage.AlphaVantageEMA.get("MSFT", AlphaVantage.Interval.`1min`, 60)
      fx.foreach { x =>
        println(s"GOT IT: ")
      }*/
      /*println("calling MACD")
      val fx2 = AlphaVantage.AlphaVantageMACD.get("MSFT", AlphaVantage.Interval.`1min`)
      fx2.foreach { x =>
        println(s"GOT IT: ${x}")
      }*/

      /*println("calling STOCHF")
      val fx3 = api.stochasticFast("MSFT", AlphaVantage.Interval.daily, 10, 3)
      fx3.foreach { x =>
        println(s"GOT IT: ${x}")
      }*/

     /* println("calling RSI")
      val fx2 = api.rsi("MSFT", AlphaVantage.Interval.daily, 2)
      fx2.foreach { x =>
        println(s"GOT IT: ${x}")
      }*/

      /*println("calling SMA")
      val fx2 = api.sma("MSFT", AlphaVantage.Interval.daily, 200)
      fx2.foreach { x =>
        println(s"GOT IT: ${x}")
      }*/


      println("calling daily")
      val fx2 = api.daily("MSFT")
      fx2.foreach { x =>
        println(s"GOT IT: ${x}")
      }
      
      Thread.currentThread.join()
/*
      val json =
        """
          |{
          |  "Meta Data": {
          |    "1: Symbol": "MSFT",
          |    "2: Indicator": "Exponential Moving Average (EMA)",
          |    "3: Last Refreshed": "2017-08-01 12:15:00",
          |    "4: Interval": "5min",
          |    "5: Time Period": 60,
          |    "6: Series Type": "close",
          |    "7: Time Zone": "US/Eastern"
          |  },
          |  "Technical Analysis: EMA": {
          |    "2017-08-01 12:15": {
          |      "EMA": "72.9170"
          |    },
          |    "2017-08-01 12:10": {
          |      "EMA": "72.9182"
          |    },
          |    "2017-08-01 12:05": {
          |      "EMA": "72.9204"
          |    }
          |  }
          |}
        """.stripMargin

      val test = Json.parse(json).as[AlphaVantage.EMAResponse]
      println(s"test: ${test}")*/
    }catch{
      case t:Throwable =>
        t.printStackTrace()
    }



   /* val json =
      """
        |{
        |     "Meta Data": {
        |         "1. Information": "Intraday (1min) prices and volumes",
        |         "2. Symbol": "MSFT",
        |         "3. Last Refreshed": "2017-07-18 16:00:00",
        |         "4. Interval": "1min",
        |         "5. Output Size": "Compact",
        |         "6. Time Zone": "US/Eastern"
        |     },
        |     "Time Series (1min)": {
        |         "2017-07-18 16:00:00": {
        |             "1. open": "73.2200",
        |             "2. high": "73.3000",
        |             "3. low": "73.2100",
        |             "4. close": "73.3000",
        |             "5. volume": "3010033"
        |         },
        |         "2017-07-18 15:59:00": {
        |             "1. open": "73.1600",
        |             "2. high": "73.2200",
        |             "3. low": "73.1500",
        |             "4. close": "73.2200",
        |             "5. volume": "255033"
        |         },
        |         "2017-07-18 15:58:00": {
        |             "1. open": "73.1250",
        |             "2. high": "73.1600",
        |             "3. low": "73.1200",
        |             "4. close": "73.1500",
        |             "5. volume": "197744"
        |         },
        |         "2017-07-18 15:57:00": {
        |             "1. open": "73.1250",
        |             "2. high": "73.1300",
        |             "3. low": "73.1200",
        |             "4. close": "73.1250",
        |             "5. volume": "115770"
        |         },
        |         "2017-07-18 15:56:00": {
        |             "1. open": "73.1200",
        |             "2. high": "73.1300",
        |             "3. low": "73.1200",
        |             "4. close": "73.1300",
        |             "5. volume": "68468"
        |         }
        |     }
        |}
      """.stripMargin
    import AlphaVantage._
    try {
      println(Json.parse(json).as[TimeSeriesResponse])
    }catch{
      case t: Throwable =>
        println(t.getMessage)
        t.printStackTrace()
    }*/

  }

}
