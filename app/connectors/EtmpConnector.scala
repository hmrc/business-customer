/*
 * Copyright 2019 HM Revenue & Customs
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
import javax.inject.Inject
import metrics.{MetricsEnum, ServiceMetrics}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultEtmpConnector @Inject()(val servicesConfig: ServicesConfig,
                                     val http: HttpClient,
                                     val auditConnector: AuditConnector,
                                     val metrics: ServiceMetrics) extends EtmpConnector {
  val serviceUrl: String = servicesConfig.baseUrl("etmp-hod")
  val registerUri = "/registration/organisation"
  val updateRegistrationDetailsUri = "/registration/safeid"
  val urlHeaderEnvironment: String = servicesConfig.getConfString("etmp-hod.environment", "")
  val urlHeaderAuthorization: String = s"Bearer ${servicesConfig.getConfString("etmp-hod.authorization-token", "")}"
}

trait EtmpConnector extends RawResponseReads with Auditable {

  def serviceUrl: String
  def registerUri: String
  def urlHeaderEnvironment: String
  def urlHeaderAuthorization: String
  def updateRegistrationDetailsUri: String
  def auditConnector: AuditConnector

  def metrics: ServiceMetrics
  def http: HttpClient
  def audit = new Audit("business-customer", auditConnector)

  def register(registerData: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    def auditRegister(registerData: JsValue, response: HttpResponse)(implicit hc: HeaderCarrier): Unit = {
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
          Logger.warn(s"[ETMPConnector][register] - status: $status")
          doFailedAudit("registerFailed", registerData.toString, response.body)
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
          Logger.warn(s"[EtmpDetailsConnector][updateRegistrationDetails] - status: $status")
          doFailedAudit("updateRegistrationDetailsFailed", updatedData.toString, response.body)
          response
      }
    }
  }

  def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))
}