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
    override val serviceUrl = ""
    override val registerUri = "/registration/organisation"
    override val http: HttpGet with HttpPost = mockWSHttp
    override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"

    override val audit: Audit = new TestAudit
    override val appName: String = "Test"

    override val metrics = Metrics
  }

  before {
    reset(mockWSHttp)
  }


  "EtmpConnector" must {

    val successResponse = Json.parse(
      """
        |{
        |  "businessName":"ACME",
        |  "businessType":"Non UK-based Company",
        |  "businessAddress":"111\nABC Street\nABC city\nABC 123\nABC",
        |  "businessTelephone":"201234567890",
        |  "businessEmail":"contact@acme.com"
        |}
      """.stripMargin)

    "use correct metrics" in {
      EtmpConnector.metrics must be(Metrics)
    }

    "for a successful registration, return registration response" in {
      val inputJsonForNUK = Json.parse(
        """
          |{
          |  "businessName": "ACME",
          |  "businessAddress": {
          |    "line_1": "111",
          |    "line_2": "ABC Street",
          |    "line_3": "ABC city",
          |    "line_4": "ABC 123",
          |    "country": "ABC"
          |  }
          |}
        """.stripMargin)

      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
        Future.successful(HttpResponse(OK, responseJson = Some(successResponse)))
      }
      val result = TestEtmpConnector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "for a failed registration, return registration response" in {
      val inputJsonForNUK = Json.parse(
        """
          |{
          |  "businessName": "ACME",
          |  "businessAddress": {
          |    "line_1": "111",
          |    "line_2": "ABC Street",
          |    "line_3": "ABC city",
          |    "line_4": "ABC 123",
          |    "country": "ABC"
          |  }
          |}
        """.stripMargin)

      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
        Future.successful(HttpResponse(BAD_REQUEST, responseJson = Some(successResponse)))
      }
      val result = TestEtmpConnector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "getDetails" must {
      "do a GET call and fetch data from ETMP for ARN that fails" in {
        val successResponseJson = Json.parse( """{"sapNumber":"1234567890", "safeId": "EX0012345678909", "agentReferenceNumber": "AARN1234567"}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestEtmpConnector.getDetails(identifier = "AARN1234567", identifierType = "arn")
        await(result).status must be(BAD_REQUEST)
      }
      "do a GET call and fetch data from ETMP for ARN" in {
        val successResponseJson = Json.parse( """{"sapNumber":"1234567890", "safeId": "EX0012345678909", "agentReferenceNumber": "AARN1234567"}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponseJson))))
        val result = TestEtmpConnector.getDetails(identifier = "AARN1234567", identifierType = "arn")
        await(result).json must be(successResponseJson)
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())
      }
      "do a GET call and fetch data from ETMP for utr" in {
        val successResponseJson = Json.parse( """{"sapNumber":"1234567890", "safeId": "EX0012345678909", "agentReferenceNumber": "AARN1234567"}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponseJson))))
        val result = TestEtmpConnector.getDetails(identifier = "1111111111", identifierType = "utr")
        await(result).json must be(successResponseJson)
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())
      }
      "do a GET call and fetch data from ETMP for safeid" in {
        val successResponseJson = Json.parse( """{"sapNumber":"1234567890", "safeId": "XP1200000100003", "agentReferenceNumber": "AARN1234567"}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponseJson))))
        val result = TestEtmpConnector.getDetails(identifier = "XP1200000100003", identifierType = "safeid")
        await(result).json must be(successResponseJson)
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())
      }
      "throw runtime exception for other identifier type" in {
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val thrown = the[RuntimeException] thrownBy TestEtmpConnector.getDetails(identifier = "AARN1234567", identifierType = "xyz")
        thrown.getMessage must include("unexpected identifier type supplied")
        verify(mockWSHttp, times(0)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())
      }
    }
  }

}
