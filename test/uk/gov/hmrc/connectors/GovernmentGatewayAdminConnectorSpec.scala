/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.connectors

import connectors.{DefaultGovernmentGatewayAdminConnector, GovernmentGatewayAdminConnector}
import metrics.ServiceMetrics
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GovernmentGatewayAdminConnectorSpec  extends PlaySpec with ConnectorTest with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach{

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val metrics: ServiceMetrics = app.injector.instanceOf[ServiceMetrics]
  val auditConnector: AuditConnector = app.injector.instanceOf[AuditConnector]
  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  class Setup extends ConnectorTest {
    val testGGConnector: GovernmentGatewayAdminConnector = new DefaultGovernmentGatewayAdminConnector(servicesConfig, metrics, mockHttpClient, auditConnector)
  }
  "GovernmentGatewayAdminConnector" must {

    val successfulJson = Json.parse( """{"rowModified":"1"}""")
    val failureJson = Json.parse( """{"error":"Constraint error"}""")

    "for successful set of known facts, return response" in new Setup {
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, successfulJson.toString)))

      val knownFacts: JsValue = Json.toJson("")
      val result: Future[HttpResponse] = testGGConnector.addKnownFacts("ATED", knownFacts)
      await(result).status must be(OK)
    }

    "for unsuccessful call of known facts, return response" in new Setup {
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, failureJson.toString)))

      val knownFacts: JsValue = Json.toJson("")
      val result: Future[HttpResponse] = testGGConnector.addKnownFacts("ATED", knownFacts)
      await(result).status must be(INTERNAL_SERVER_ERROR)
    }
  }
}
