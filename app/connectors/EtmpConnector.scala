/*
 * Copyright 2021 HM Revenue & Customs
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
import metrics.{MetricsEnum, ServiceMetrics}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
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

trait EtmpConnector extends RawResponseReads with Auditable with Logging {

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
    def auditRegister(registerData: JsValue, response: HttpResponse): Unit = {
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

    val timerContext = metrics.startTimer(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
    http.POST(s"$serviceUrl$registerUri", registerData, createHeaders).map { response =>
      timerContext.stop()
      auditRegister(registerData, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          logger.warn(s"[ETMPConnector][register] - status: $status")
          doFailedAudit("registerFailed", registerData.toString, response.body)
          response
      }
    }
  }

  def updateRegistrationDetails(safeId: String, updatedData: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    def auditUpdateRegistrationDetails(safeId: String,
                                               updateData: JsValue,
                                               response: HttpResponse) {
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

    val putUrl = s"""$serviceUrl$updateRegistrationDetailsUri/$safeId"""
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
    http.PUT(putUrl, updatedData, createHeaders).map { response =>
      timerContext.stop()
      auditUpdateRegistrationDetails(safeId, updatedData, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_UPDATE_REGISTRATION_DETAILS)
          logger.warn(s"[EtmpDetailsConnector][updateRegistrationDetails] - status: $status")
          doFailedAudit("updateRegistrationDetailsFailed", updatedData.toString, response.body)
          response
      }
    }
  }

  def createHeaders: Seq[(String, String)] = {
    Seq(
      "Environment"   -> urlHeaderEnvironment,
      "Authorization" -> urlHeaderAuthorization
    )
  }
}