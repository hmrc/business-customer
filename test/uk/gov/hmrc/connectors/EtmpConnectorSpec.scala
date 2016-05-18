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

package uk.gov.hmrc.connectors

import java.util.UUID

import connectors.EtmpConnector
import metrics.Metrics
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.audit.TestAudit
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class EtmpConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestAuditConnector extends AuditConnector with AppName with RunMode {
    override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
  }

  class MockHttp extends WSGet with WSPost {
    override val hooks = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]

  object TestEtmpConnector extends EtmpConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
    override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"

    override val audit: Audit = new TestAudit
    override val appName: String = "Test"

    override def metrics = Metrics
  }

  before {
    reset(mockWSHttp)
  }


  "EtmpConnector" must {

    val successResponse = Json.parse( """{"businessName":"ACME","businessType":"Non UK-based Company","businessAddress":"111\nABC Street\nABC city\nABC 123\nABC","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
    "use correct metrics" in {
      EtmpConnector.metrics must be(Metrics)
    }

    "for a successful registration, return registration response" in {
      val inputJsonForNUK = Json.parse( """{ "businessName": "ACME", "businessAddress": {"line_1": "111", "line_2": "ABC Street", "line_3": "ABC city", "line_4": "ABC 123", "country": "ABC"} }""")

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(successResponse))))
      val result = TestEtmpConnector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "for a failed registration, return registration response" in {
      val inputJsonForNUK = Json.parse( """{ "businessName": "ACME", "businessAddress": {"line_1": "111", "line_2": "ABC Street", "line_3": "ABC city", "line_4": "ABC 123", "country": "ABC"} }""")

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, responseJson = Some(successResponse))))
      val result = TestEtmpConnector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }
  }

}
