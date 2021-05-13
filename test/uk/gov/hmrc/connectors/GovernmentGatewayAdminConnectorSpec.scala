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

package uk.gov.hmrc.connectors

import connectors.GovernmentGatewayAdminConnector
import metrics.ServiceMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.audit.TestAudit
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import java.util.UUID
import scala.concurrent.Future

class GovernmentGatewayAdminConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockWSHttp: HttpClient = mock[HttpClient]
  val mockServiceMetrics: ServiceMetrics = app.injector.instanceOf[ServiceMetrics]

  trait Setup {
    class TestGGAdminConnector extends GovernmentGatewayAdminConnector {
      override val serviceUrl = ""
      override val ggaBaseUrl = ""
      override val http: HttpClient = mockWSHttp

      override val audit: Audit = new TestAudit(app.injector.instanceOf[AuditConnector])
      override def metrics: ServiceMetrics = mockServiceMetrics
    }

    val connector = new TestGGAdminConnector()
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "GovernmentGatewayAdminConnector" must {

    val successfulJson = Json.parse( """{"rowModified":"1"}""")
    val failureJson = Json.parse( """{"error":"Constraint error"}""")

    "for successful set of known facts, return response" in new Setup {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(OK, successfulJson.toString)))

      val knownFacts = Json.toJson("")
      val result = connector.addKnownFacts("ATED", knownFacts)
      await(result).status must be(OK)
    }

    "for unsuccessful call of known facts, return response" in new Setup {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, failureJson.toString)))

      val knownFacts = Json.toJson("")
      val result = connector.addKnownFacts("ATED", knownFacts)
      await(result).status must be(INTERNAL_SERVER_ERROR)
    }

  }

}
