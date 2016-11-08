/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import audit.Auditable
import config.{MicroserviceAuditConnector, WSHttp}
import metrics.{Metrics, MetricsEnum}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EtmpConnector extends ServicesConfig with Auditable {

  def serviceUrl: String

  def registerUri: String

  def urlHeaderEnvironment: String

  def urlHeaderAuthorization: String

  def metrics: Metrics

  def http: HttpGet with HttpPost

  def register(registerData: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
    http.POST(s"$serviceUrl$registerUri", registerData).map { response =>
      timerContext.stop()
      auditRegister(registerData, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          Logger.warn(s"[ETMPConnector][register] - status: $status Error: ${response.body}")
          response
      }
    }
  }

  def getDetails(identifier: String, identifierType: String): Future[HttpResponse] = {
    def getDetailsFromEtmp(getUrl: String): Future[HttpResponse] = {
      implicit val hc = createHeaderCarrier
      Logger.debug(s"[EtmpDetailsConnector][getDetailsFromEtmp] - GET $getUrl")
      val timerContext = metrics.startTimer(MetricsEnum.ETMP_GET_DETAILS)
      http.GET[HttpResponse](getUrl).map { response =>
        timerContext.stop()
        response.status match {
          case OK => metrics.incrementSuccessCounter(MetricsEnum.ETMP_GET_DETAILS)
          case status =>
            metrics.incrementFailedCounter(MetricsEnum.ETMP_GET_DETAILS)
            Logger.warn(s"[EtmpDetailsConnector][getDetailsFromEtmp] - status: $status Error ${response.body}")
        }
        response
      }
    }

    identifierType match {
      case "arn" => getDetailsFromEtmp(s"$serviceUrl/registration/details?arn=$identifier")
      case "safeid" => getDetailsFromEtmp(s"$serviceUrl/registration/details?safeid=$identifier")
      case "utr" => getDetailsFromEtmp(s"$serviceUrl/registration/details?utr=$identifier")
      case unknownIdentifier =>
        Logger.warn(s"[EtmpDetailsConnector][getDetails] - unexpected identifier type supplied of $unknownIdentifier")
        throw new RuntimeException(s"[EtmpDetailsConnector][getDetails] - unexpected identifier type supplied of $unknownIdentifier")
    }
  }

  private def auditRegister(registerData: JsValue, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "etmpRegister",
      detail = Map("txName" -> "etmpRegister",
        "registerData" -> s"$registerData",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

  def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

}

object EtmpConnector extends EtmpConnector {
  val serviceUrl = baseUrl("etmp-hod")
  val registerUri = "/registration/organisation"
  val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
  val metrics = Metrics
  val http = WSHttp
  val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  val appName: String = AppName.appName
}
