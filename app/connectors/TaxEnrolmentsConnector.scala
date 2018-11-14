/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.concurrent.ExecutionContext.Implicits.global

trait TaxEnrolmentsConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def emacBaseUrl: String

  def metrics: Metrics

  def http: CorePut

  def addKnownFacts(serviceName: String, knownFacts: JsValue, arn: String)(implicit hc: HeaderCarrier) = {

    val agentRefIdentifier = "AgentRefNumber"
    val enrolmentKey = s"$serviceName~$agentRefIdentifier~$arn" // TODO: refactor to createEnrolmentKey method
    val putUrl = s"$emacBaseUrl/$enrolmentKey"

    val timerContext = metrics.startTimer(MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS)
    http.PUT[JsValue, HttpResponse](putUrl, knownFacts) map { response =>
      timerContext.stop()
      auditAddKnownFacts(putUrl, serviceName, knownFacts, response)
      response.status match {
        case NO_CONTENT =>
          metrics.incrementSuccessCounter(MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.EMAC_ADMIN_ADD_KNOWN_FACTS)
          Logger.warn(s"[TaxEnrolmentsConnector][addKnownFacts] - status: $status")
          doFailedAudit("addKnownFacts", knownFacts.toString, response.body)
          response
      }
    }
  }

  private def auditAddKnownFacts(putUrl: String,
                                 serviceName: String,
                                 knownFacts: JsValue,
                                 response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val status = response.status match {
      case NO_CONTENT => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "emacAddKnownFactsES06",
      detail = Map("txName" -> "emacAddKnownFacts",
        "serviceName" -> s"$serviceName",
        "putUrl" -> s"$putUrl",
        "requestBody" -> s"$knownFacts",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$status"))
  }

}

object TaxEnrolmentsConnector extends TaxEnrolmentsConnector {
  val serviceUrl = baseUrl("tax-enrolments")
  val emacBaseUrl = s"$serviceUrl/tax-enrolments/enrolments"
  val metrics = Metrics
  val http = WSHttp
  val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  val appName: String = AppName.appName
}
