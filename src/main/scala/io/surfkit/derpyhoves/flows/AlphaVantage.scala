package io.surfkit.derpyhoves.flows

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import play.api.libs.json._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import org.joda.time.DateTimeZone

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try
import com.typesafe.config.ConfigFactory
/**
  * Created by suroot on 18/07/17.
  */
object AlphaVantage{
  sealed trait AV

  case class Interval(period: String) extends AV{
    def toDuration = this match{
      case AlphaVantage.Interval.`1min` => 1 minute
      case AlphaVantage.Interval.`5min` => 5 minutes
      case AlphaVantage.Interval.`15min` => 15 minutes
      case AlphaVantage.Interval.`30min` => 30 minutes
      case AlphaVantage.Interval.`60min` => 60 minutes
      case AlphaVantage.Interval.daily => 1 day
      case AlphaVantage.Interval.weekly => 7 days
      case AlphaVantage.Interval.monthly => 30 days
      case _ => 24 hours
    }
  }
  object Interval{
    val `1min` = Interval("1min")
    val `5min` = Interval("5min")
    val `15min` = Interval("15min")
    val `30min` = Interval("30min")
    val `60min` = Interval("60min")
    val daily = Interval("daily")
    val weekly = Interval("weekly")
    val monthly = Interval("monthly")
  }

  final case class IntervalPrice(
                `1. open`: String,
                `2. high`: String,
                `3. low`: String,
                `4. close`: String,
                `5. volume`: String) extends AV
  implicit val tickWrites = Json.writes[IntervalPrice]
  implicit val tickReads = Json.reads[IntervalPrice]

  final case class TimeSeries(series: Seq[(String, IntervalPrice)]) extends AV
  implicit val tsFormat: Format[TimeSeries] =
    new Format[TimeSeries] {
      override def reads(json: JsValue): JsResult[TimeSeries] = json match {
        case j: JsObject =>
          JsSuccess(TimeSeries(j.fields.map {
            case (name, size) =>
              size.validate[IntervalPrice] match {
                case JsSuccess(validSize, _) => (name, validSize)
                case e: JsError => return e
              }
          }))
        case _ =>
          JsError("Invalid JSON type")
      }

      override def writes(o: TimeSeries): JsValue = Json.toJson(o.series.toMap)
    }

  case class MetaData(
                 `1. Information`: String,
                 `2. Symbol`: String,
                 `3. Last Refreshed`: String,
                 `4. Interval`: String,
                 `5. Output Size`: String,
                 `6. Time Zone`: String
   ) extends AV
  implicit val mdWrites = Json.writes[MetaData]
  implicit val mdReads = Json.reads[MetaData]

  case class MetaDataDaily(
                       `1. Information`: String,
                       `2. Symbol`: String,
                       `3. Last Refreshed`: String,
                       `4. Output Size`: String,
                       `5. Time Zone`: String
                     ) extends AV
  implicit val mddWrites = Json.writes[MetaDataDaily]
  implicit val mddReads = Json.reads[MetaDataDaily]

  case class MetaDataMA(
                          `1: Symbol`: String,
                          `2: Indicator`: String,
                          `3: Last Refreshed`: String,
                          `4: Interval`: String,
                          `5: Time Period`: Int,
                          `6: Series Type`: String,
                          `7: Time Zone`: String
                          ) extends AV
  implicit val mddEMAWrites = Json.writes[MetaDataMA]
  implicit val mddEMAReads = Json.reads[MetaDataMA]
  // SMA
  case class SMA(SMA: String) extends AV
  implicit val smaWrites = Json.writes[SMA]
  implicit val smaReads = Json.reads[SMA]

  final case class SMASeries(sma: Seq[(String, SMA)]) extends AV
  implicit val tsSmaFormat: Format[SMASeries] =
    new Format[SMASeries] {
      override def reads(json: JsValue): JsResult[SMASeries] = json match {
        case j: JsObject =>
          JsSuccess(SMASeries(j.fields.map {
            case (name, size) =>
              size.validate[SMA] match {
                case JsSuccess(validSize, _) =>
                  (name, validSize)
                case e: JsError =>
                  return e
              }
          }))
        case _ =>
          JsError("Invalid JSON type")
      }
      override def writes(o: SMASeries): JsValue = Json.toJson(o.sma.toMap)
    }

  case class SMAResponse(`Meta Data`: MetaDataMA, `Technical Analysis: SMA`: SMASeries) extends AV
  implicit val smarWrites = Json.writes[SMAResponse]
  implicit val smarReads = Json.reads[SMAResponse]


  // EMA
  case class EMA(EMA: String) extends AV
  implicit val emaWrites = Json.writes[EMA]
  implicit val eamReads = Json.reads[EMA]

  final case class EMASeries(ema: Seq[(String, EMA)]) extends AV
  implicit val tsEmaFormat: Format[EMASeries] =
    new Format[EMASeries] {
      override def reads(json: JsValue): JsResult[EMASeries] = json match {
        case j: JsObject =>
          JsSuccess(EMASeries(j.fields.map {
            case (name, size) =>
              size.validate[EMA] match {
                case JsSuccess(validSize, _) =>
                  (name, validSize)
                case e: JsError =>
                  return e
              }
          }))
        case _ =>
          JsError("Invalid JSON type")
      }
      override def writes(o: EMASeries): JsValue = Json.toJson(o.ema.toMap)
    }

  case class EMAResponse(`Meta Data`: MetaDataMA, `Technical Analysis: EMA`: EMASeries) extends AV
  implicit val emarWrites = Json.writes[EMAResponse]
  implicit val emarReads = Json.reads[EMAResponse]

  case class MetaDataMACD(
                          `1: Symbol`: String,
                          `2: Indicator`: String,
                          `3: Last Refreshed`: String,
                          `4: Interval`: String,
                          `5.1: Fast Period`: Int,
                          `5.2: Slow Period`: Int,
                          `5.3: Signal Period`: Int,
                          `6: Series Type`: String,
                          `7: Time Zone`: String
                        ) extends AV
  implicit val mddMACDWrites = Json.writes[MetaDataMACD]
  implicit val mddMACDReads = Json.reads[MetaDataMACD]

  case class MACD(MACD: String, MACD_Signal: String, MACD_Hist: String) extends AV
  implicit val macdWrites = Json.writes[MACD]
  implicit val macdReads = Json.reads[MACD]

  final case class MACDSeries(macd: Seq[(String, MACD)]) extends AV
  implicit val tsMacdFormat: Format[MACDSeries] =
    new Format[MACDSeries] {
      override def reads(json: JsValue): JsResult[MACDSeries] = json match {
        case j: JsObject =>
          JsSuccess(MACDSeries(j.fields.map {
            case (name, size) =>
              size.validate[MACD] match {
                case JsSuccess(validSize, _) =>
                  (name, validSize)
                case e: JsError => return e
              }
          }))
        case _ => JsError("Invalid JSON type")
      }
      override def writes(o: MACDSeries): JsValue = Json.toJson(o.macd.toMap)
    }

  case class MACDResponse(`Meta Data`: MetaDataMACD, `Technical Analysis: MACD`: MACDSeries) extends AV
  implicit val macdrWrites = Json.writes[MACDResponse]
  implicit val macdrReads = Json.reads[MACDResponse]



  case class MaType(code: Int) extends AV
  object MaType{
    val SMA = MaType(0)
    val EMA = MaType(1)
    val WMA = MaType(2)
    val DEMA = MaType(3)
    val TEMA =  MaType(4)
    val TRIMA = MaType(5)
    val T3 = MaType(6)
    val KAMA = MaType(7)
    val MESA = MaType(8)
  }

  // STOCHF
  case class MetaDataSTOCHF(
                           `1: Symbol`: String,
                           `2: Indicator`: String,
                           `3: Last Refreshed`: String,
                           `4: Interval`: String,
                           `5.1: FastK Period`: Int,
                           `5.2: FastD Period`: Int,
                           `5.3: FastD MA Type`: Int,
                           `6: Time Zone`: String
                         ) extends AV
  implicit val mddSTOCHFWrites = Json.writes[MetaDataSTOCHF]
  implicit val mddSTOCHFReads = Json.reads[MetaDataSTOCHF]

  case class STOCHF(FastD: String, FastK: String) extends AV
  implicit val stochFWrites = Json.writes[STOCHF]
  implicit val stochFReads = Json.reads[STOCHF]

  final case class STOCHFSeries(stochf: Seq[(String, STOCHF)]) extends AV
  implicit val tsStochfFormat: Format[STOCHFSeries] =
    new Format[STOCHFSeries] {
      override def reads(json: JsValue): JsResult[STOCHFSeries] = json match {
        case j: JsObject =>
          JsSuccess(STOCHFSeries(j.fields.map {
            case (name, size) =>
              size.validate[STOCHF] match {
                case JsSuccess(validSize, _) => (name, validSize)
                case e: JsError => return e
              }
          }))
        case _ => JsError("Invalid JSON type")
      }
      override def writes(o: STOCHFSeries): JsValue = Json.toJson(o.stochf.toMap)
    }

  case class STOCHFResponse(`Meta Data`: MetaDataSTOCHF, `Technical Analysis: STOCHF`: STOCHFSeries) extends AV
  implicit val stochfrWrites = Json.writes[STOCHFResponse]
  implicit val stochfrReads = Json.reads[STOCHFResponse]



  // RSI
  case class MetaDataRSI(
                             `1: Symbol`: String,
                             `2: Indicator`: String,
                             `3: Last Refreshed`: String,
                             `4: Interval`: String,
                             `5: Time Period`: Int,
                             `6: Series Type`: String,
                             `7: Time Zone`: String
                           ) extends AV
  implicit val mddRSIWrites = Json.writes[MetaDataRSI]
  implicit val mddRSIReads = Json.reads[MetaDataRSI]

  case class RSI(RSI: String) extends AV
  implicit val rsiWrites = Json.writes[RSI]
  implicit val rsiFReads = Json.reads[RSI]

  final case class RSISeries(rsi: Seq[(String, RSI)]) extends AV
  implicit val tsRsiFormat: Format[RSISeries] =
    new Format[RSISeries] {
      override def reads(json: JsValue): JsResult[RSISeries] = json match {
        case j: JsObject =>
          JsSuccess(RSISeries(j.fields.map {
            case (name, size) =>
              size.validate[RSI] match {
                case JsSuccess(validSize, _) => (name, validSize)
                case e: JsError => return e
              }
          }))
        case _ => JsError("Invalid JSON type")
      }
      override def writes(o: RSISeries): JsValue = Json.toJson(o.rsi.toMap)
    }

  case class RSIResponse(`Meta Data`: MetaDataRSI, `Technical Analysis: RSI`: RSISeries) extends AV
  implicit val rsirWrites = Json.writes[RSIResponse]
  implicit val rsirReads = Json.reads[RSIResponse]



  trait TsResponse extends AV{
    def `Time Series`: TimeSeries
  }

  final case class TimeSeriesResponse(
                            `Meta Data`: MetaData,
                               `Time Series`: TimeSeries
                               ) extends TsResponse
  implicit val tsrFormat: Format[TimeSeriesResponse] =
    new Format[TimeSeriesResponse] {
      override def reads(json: JsValue): JsResult[TimeSeriesResponse] = json match {
        case j: JsObject =>
          val meta = j.fields.find(_._1 == "Meta Data").get._2.validate[MetaData]
          val ts = j.fields.find(_._1.startsWith("Time Series")).get._2.validate[TimeSeries]
          (meta, ts) match{
            case (JsSuccess(m, _), JsSuccess(t, _)) => JsSuccess(TimeSeriesResponse(m, t))
            case (JsError(e), _) => println(s"error: ${e}"); JsError(e)
            case (_, JsError(e)) => println(s"error: ${e}"); JsError(e)
          }
        case _ =>
          println("error")
          JsError("Invalid JSON type")
      }

      override def writes(o: TimeSeriesResponse): JsValue = Json.toJson(o)
    }


  final case class TimeSeriesDailyResponse(
                                       `Meta Data`: MetaDataDaily,
                                       `Time Series`: TimeSeries
                                     ) extends TsResponse
  implicit val tsdrFormat: Format[TimeSeriesDailyResponse] =
    new Format[TimeSeriesDailyResponse] {
      override def reads(json: JsValue): JsResult[TimeSeriesDailyResponse] = json match {
        case j: JsObject =>
          val meta = j.fields.find(_._1 == "Meta Data").get._2.validate[MetaDataDaily]
          val ts = j.fields.find(_._1.startsWith("Time Series")).get._2.validate[TimeSeries]
          (meta, ts) match{
            case (JsSuccess(m, _), JsSuccess(t, _)) => JsSuccess(TimeSeriesDailyResponse(m, t))
            case (JsError(e), _) => println(s"error: ${e}"); JsError(e)
            case (_, JsError(e)) => println(s"error: ${e}"); JsError(e)
          }

        case _ =>
          println("error")
          JsError("Invalid JSON type")
      }

      override def writes(o: TimeSeriesDailyResponse): JsValue = Json.toJson(o)
    }


  object FullTimeSeries extends PlayJsonSupport {
    def get(symbol: String, interval: AlphaVantage.Interval, apiKey: String)(implicit system: ActorSystem, materializer: Materializer, um: Reads[AlphaVantage.TimeSeriesResponse]): Future[AlphaVantage.TsResponse] =
      if(interval.period.contains("min")) {
        val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=${symbol}&outputsize=full&interval=${interval.period}&apikey=${apiKey}"
        println(s"curl -XGET '${url}'")
        Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
          Unmarshal(response.entity).to[AlphaVantage.TimeSeriesResponse]
        }
      }else{
        val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=${symbol}&outputsize=full&apikey=${apiKey}"
        println(s"curl -XGET '${url}'")
        Http().singleRequest(HttpRequest(uri = url)).flatMap { response =>
          Unmarshal(response.entity).to[AlphaVantage.TimeSeriesDailyResponse]
        }
      }
  }



  // Slack Helper classes ...
  case class SlackField(
              title: String,
              value: String,
              short: Boolean ) extends AV
  implicit val SlackFieldWrites = Json.writes[SlackField]
  implicit val SlackFieldReads = Json.reads[SlackField]

  case class SlackAttachment(
            fallback: String,
            title: String,
            text: String,
            color: Option[String] = None,
            pretext: Option[String] = None,
            author_name: Option[String] = None,
            author_link: Option[String] = None,
            author_icon: Option[String] = None,
            title_link: Option[String] = None,
            fields: Seq[SlackField] = Seq.empty,
            image_url: Option[String] = None,
            thumb_url: Option[String] = None,
            footer: Option[String] = None,
            footer_icon: Option[String] = None,
            ts: Option[Long] = None) extends AV
  implicit val SlackAttachmentWrites = Json.writes[SlackAttachment]
  implicit val SlackAttachmentReads = Json.reads[SlackAttachment]

  case class SlackAttachments(attachments: Seq[SlackAttachment]) extends AV
  implicit val SlackAttachmentsWrites = Json.writes[SlackAttachments]
  implicit val SlackAttachmentsReads = Json.reads[SlackAttachments]

}

class AlphaVantage[T <: AlphaVantage.AV](function: String,symbol: String, interval: AlphaVantage.Interval, tz: DateTimeZone, apiKey: String, fuzz: Double = 5.0)(implicit system: ActorSystem, materializer: Materializer, um: Reads[T]) extends TimeSeries(
  url = s"https://www.alphavantage.co/query?function=${function}&symbol=${symbol}&interval=${interval.period}&apikey=${apiKey}",
  interval = interval.toDuration, Some(tz), fuzz) with PlayJsonSupport{

  def json(): Source[Try[Future[T]], Cancellable] = super.apply().map{
    case scala.util.Success(response) => scala.util.Success(Unmarshal(response.entity).to[T])
    case scala.util.Failure(ex) => scala.util.Failure(ex)
  }
}

case class AlphaVantageTimeSeries(symbol: String, interval: AlphaVantage.Interval, tz: DateTimeZone, apiKey: String, fuzz: Double = 5.0)(implicit system: ActorSystem, materializer: Materializer)
  extends AlphaVantage[AlphaVantage.TimeSeriesResponse]("TIME_SERIES_INTRADAY", symbol, interval, tz, apiKey)




class AlphaVantageApi(apikey: String)(implicit system: ActorSystem, materializer: Materializer, ex: ExecutionContext) extends PlayJsonSupport {

  val baseUrl = "https://www.alphavantage.co/query?function="

  def http(url: String) = Http().singleRequest(HttpRequest(uri = url))

  def unmarshal[T <: AlphaVantage.AV](response: HttpResponse)(implicit um: Reads[T]):Future[T] = Unmarshal(response.entity).to[T]

  def post[T <: AlphaVantage.AV](url: String, post: T)(implicit uw: Writes[T]) = {
    val json = Json.stringify(uw.writes(post))
    val jsonEntity = HttpEntity(ContentTypes.`application/json`, json)
    println(s"curl -XPOST '${url}' -d '${json}'")
    Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"${url}", entity = jsonEntity))

  }

  // API FUNCTIONS
  def stochasticFast(symbol: String, interval: AlphaVantage.Interval, fastK: Int, fastD: Int, matype: AlphaVantage.MaType = AlphaVantage.MaType.SMA)(implicit um: Reads[AlphaVantage.STOCHFResponse]) =
    http(baseUrl + s"STOCHF&symbol=${symbol}&fastkperiod=${fastK}&fastdperiod=${fastD}&fastdmatype=${matype.code}&interval=${interval.period}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def rsi(symbol: String, interval: AlphaVantage.Interval, time_period: Int)(implicit um: Reads[AlphaVantage.RSIResponse]) =
    http(baseUrl + s"RSI&series_type=close&symbol=${symbol}&interval=${interval.period}&time_period=${time_period}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def macd(symbol: String, interval: AlphaVantage.Interval)(implicit um: Reads[AlphaVantage.MACDResponse]) =
    http(baseUrl + s"MACD&series_type=close&symbol=${symbol}&interval=${interval.period}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def ema(symbol: String, interval: AlphaVantage.Interval, timePeriod: Int)(implicit um: Reads[AlphaVantage.EMAResponse]) =
    http(baseUrl + s"EMA&time_period=${timePeriod}&series_type=close&symbol=${symbol}&interval=${interval.period}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def sma(symbol: String, interval: AlphaVantage.Interval, timePeriod: Int)(implicit um: Reads[AlphaVantage.SMAResponse]) =
    http(baseUrl + s"SMA&time_period=${timePeriod}&series_type=close&symbol=${symbol}&interval=${interval.period}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def daily(symbol: String, outputsize: String = "compact")(implicit um: Reads[AlphaVantage.TimeSeriesDailyResponse]) =
    http(baseUrl + s"TIME_SERIES_DAILY&symbol=${symbol}&outputsize=${outputsize}&apikey=${apikey}").flatMap(x => unmarshal(x) )

  def sendSlack(webhookUrl: String, attachments: AlphaVantage.SlackAttachments) = post(webhookUrl, attachments)
}



