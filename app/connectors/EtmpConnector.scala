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
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait EtmpConnector extends ServicesConfig with Auditable {

  def serviceUrl: String

  def registerUri: String

  def urlHeaderEnvironment: String

  def urlHeaderAuthorization: String

  def updateRegistrationDetailsUri: String

  def metrics: Metrics

  def http: HttpGet with HttpPost with HttpPut

  def register(registerData: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    def auditRegister(registerData: JsValue, response: HttpResponse)(implicit hc: HeaderCarrier) = {
      val status = response.status match {
        case OK => EventTypes.Succeeded
        case _ => EventTypes.Failed
      }
      sendDataEvent(transactionName = "etmpRegister",
        detail = Map("txName" -> "etmpRegister",
          "registerData" -> s"$registerData",
          "responseStatus" -> s"${response.status}",
          "responseBody" -> s"${response.body}",
          "status" -> s"$status"
        ))
    }

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

  def updateRegistrationDetails(safeId: String, updatedData: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    def auditUpdateRegistrationDetails(safeId: String,
                                               updateData: JsValue,
                                               response: HttpResponse)(implicit hc: HeaderCarrier) {
      val status = response.status match {
        case OK => EventTypes.Succeeded
        case _ => EventTypes.Failed
      }
      sendDataEvent(transactionName = "etmpUpdateRegistrationDetails",
        detail = Map("txName" -> "etmpUpdateRegistrationDetails",
          "safeId" -> s"$safeId",
          "requestData" -> s"${Json.toJson(updateData)}",
          "responseStatus" -> s"${response.status}",
          "responseBody" -> s"${response.body}",
          "status" -> s"$status"))
    }

    implicit val hc = createHeaderCarrier
    val putUrl = s"""$serviceUrl$updateRegistrationDetailsUri/$safeId"""
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
    http.PUT(putUrl, updatedData).map { response =>
      timerContext.stop()
      auditUpdateRegistrationDetails(safeId, updatedData, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
          Logger.warn(s"[EtmpDetailsConnector][updateRegistrationDetails] - status: $status Error ${response.body}")
          response
      }
    }
  }


  def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

}

object EtmpConnector extends EtmpConnector {
  val serviceUrl = baseUrl("etmp-hod")
  val registerUri = "/registration/organisation"
  val updateRegistrationDetailsUri = "/registration/safeid"
  val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
  val metrics = Metrics
  val http = WSHttp
  val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  val appName: String = AppName.appName
}
