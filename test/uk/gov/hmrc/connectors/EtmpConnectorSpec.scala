/*
 * Copyright 2020 HM Revenue & Customs
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
import metrics.ServiceMetrics
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.audit.TestAudit
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class EtmpConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val mockWSHttp: HttpClient = mock[HttpClient]
  val mockServiceMetrics: ServiceMetrics = app.injector.instanceOf[ServiceMetrics]

  trait Setup {
    class TestEtmpConnector extends EtmpConnector {
      override val serviceUrl = ""
      override val registerUri = "/registration/organisation"
      override val updateRegistrationDetailsUri = "/registration/safeid"
      override val http: HttpClient = mockWSHttp
      override val urlHeaderEnvironment: String = ""
      override val urlHeaderAuthorization: String = ""
      override def auditConnector: AuditConnector = app.injector.instanceOf[AuditConnector]
      override val audit: Audit = new TestAudit(auditConnector)
      override val metrics: ServiceMetrics = mockServiceMetrics
    }

    val connector = new TestEtmpConnector()
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

    "for a successful registration, return registration response" in new Setup {
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
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn {
        Future.successful(HttpResponse(OK, responseJson = Some(successResponse)))
      }
      val result = connector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "for a failed registration, return registration response" in new Setup {
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
      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn {
        Future.successful(HttpResponse(BAD_REQUEST, responseJson = Some(successResponse)))
      }
      val result = connector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "update registration details" must {
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


      "Correctly submit data if with a valid response" in new Setup {
        val successResponse = Json.parse( """{"processingDate": "2001-12-17T09:30:47Z"}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = connector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response = await(result)
        response.status must be(OK)
        response.json must be(successResponse)
      }

      "submit data  with an invalid response" in new Setup {
        val notFoundResponse = Json.parse( """{}""")
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))

        val result = connector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response = await(result)
        response.status must be(NOT_FOUND)
      }
    }
  }

}
