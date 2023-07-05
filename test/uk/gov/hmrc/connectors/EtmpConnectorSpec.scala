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

import connectors.EtmpConnector
import metrics.ServiceMetrics
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.audit.TestAudit
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import scala.concurrent.{ExecutionContext, Future}

class EtmpConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val mockWSHttp: HttpClient = mock[HttpClient]
  val mockServiceMetrics: ServiceMetrics = app.injector.instanceOf[ServiceMetrics]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    class TestEtmpConnector extends EtmpConnector {
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
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
      val inputJsonForNUK: JsValue = Json.parse(
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

      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn {
        Future.successful(HttpResponse(OK, successResponse.toString))
      }
      val result: Future[HttpResponse] = connector.register(inputJsonForNUK)
      await(result).json must be(successResponse)
    }

    "for a failed registration, return registration response" in new Setup {
      val inputJsonForNUK: JsValue = Json.parse(
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

      when(mockWSHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any())) thenReturn {
        Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString))
      }
      val result: Future[HttpResponse] = connector.register(inputJsonForNUK)
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
        val successResponse: JsValue = Json.parse( """{"processingDate": "2001-12-17T09:30:47Z"}""")

        when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response: HttpResponse = await(result)
        response.status must be(OK)
        response.json must be(successResponse)
      }

      "submit data  with an invalid response" in new Setup {
        val notFoundResponse: JsValue = Json.parse( """{}""")

        when(mockWSHttp.PUT[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, notFoundResponse.toString)))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response: HttpResponse = await(result)
        response.status must be(NOT_FOUND)
      }
    }
  }

}
