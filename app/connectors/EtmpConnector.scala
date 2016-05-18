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

  lazy val serviceURL = baseUrl("etmp-hod")
  val baseURI = ""
  val registerURI = "/registration/organisation"
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String

  def metrics: Metrics

  val http: HttpGet with HttpPost = WSHttp

  def register(registerData: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val headerCarrier = createHeaderCarrier
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
    http.POST(s"""$serviceURL$baseURI$registerURI""", registerData).map { response =>
      val stopContext = timerContext.stop()
      auditRegister(registerData, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          response
        case status => {
          metrics.incrementFailedCounter(MetricsEnum.ETMP_REGISTER_BUSINESS_PARTNER)
          Logger.warn(s"[ETMPConnector][register] - status: $status Error: ${response.body}")
          response
        }
      }
    }
  }

  private def auditRegister(registerData: JsValue, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "etmpRegister",
      detail = Map("txName" -> "etmpRegister",
        "registerData" -> s"${registerData}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}"),
      eventType = eventType)
  }

  def createHeaderCarrier(): HeaderCarrier = {
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))
  }
}

object EtmpConnector extends EtmpConnector {
  override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"

  override val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  override val appName: String = AppName.appName

  override def metrics = Metrics
}
