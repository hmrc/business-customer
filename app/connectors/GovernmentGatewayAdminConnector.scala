/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultGovernmentGatewayAdminConnector @Inject()(val servicesConfig: ServicesConfig,
                                                       val metrics: ServiceMetrics,
                                                       val http: HttpClient,
                                                       val auditConnector: AuditConnector) extends GovernmentGatewayAdminConnector {
  val serviceUrl: String = servicesConfig.baseUrl("government-gateway-admin")
  val ggaBaseUrl = s"$serviceUrl/government-gateway-admin/service"
  val audit: Audit = new Audit("business-customer", auditConnector)
}

trait GovernmentGatewayAdminConnector extends RawResponseReads with Auditable with Logging {

  def serviceUrl: String
  def ggaBaseUrl: String
  def metrics: ServiceMetrics
  def http: HttpClient

  def addKnownFacts(serviceName: String, knownFacts: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val postUrl = s"$ggaBaseUrl/$serviceName/known-facts"
    val timerContext = metrics.startTimer(MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS)
    http.POST[JsValue, HttpResponse](postUrl, knownFacts, Seq.empty) map { response =>
      timerContext.stop()
      auditAddKnownFacts(serviceName, knownFacts, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.GG_ADMIN_ADD_KNOWN_FACTS)
          logger.warn(s"[GovernmentGatewayAdminConnector][addKnownFacts] - status: $status")
          doFailedAudit("addKnownFacts", knownFacts.toString, response.body)
          response
      }
    }
  }

  private def auditAddKnownFacts(serviceName: String, knownFacts: JsValue, response: HttpResponse)(implicit hc: HeaderCarrier): Unit = {
    val status: String = response.status match {
      case OK => EventTypes.Succeeded
      case _  => EventTypes.Failed
    }
    sendDataEvent(transactionName = "ggAddKnownFacts",
      detail = Map("txName" -> "ggAddKnownFacts",
        "serviceName" -> s"$serviceName",
        "knownFacts" -> s"$knownFacts",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$status"))
  }
}
