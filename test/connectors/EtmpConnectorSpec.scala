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

package connectors

import metrics.ServiceMetrics
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EtmpConnectorSpec extends PlaySpec with ConnectorTest with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach{
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val metrics: ServiceMetrics = app.injector.instanceOf[ServiceMetrics]
  val auditConnector: AuditConnector = app.injector.instanceOf[AuditConnector]
  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  class Setup extends ConnectorTest {
    val testEtmpConnector: EtmpConnector = new DefaultEtmpConnector(servicesConfig, mockHttpClient, auditConnector, metrics)
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

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))
      val result: Future[HttpResponse] = testEtmpConnector.register(inputJsonForNUK)
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

      when(requestBuilderExecute[HttpResponse]).thenReturn(
        Future.successful(HttpResponse(BAD_REQUEST, successResponse.toString)))

      val result: Future[HttpResponse] = testEtmpConnector.register(inputJsonForNUK)
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
        val successResponse: JsValue = Json.parse("""{"processingDate": "2001-12-17T09:30:47Z"}""")

        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = testEtmpConnector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response: HttpResponse = await(result)
        response.status must be(OK)
        response.json must be(successResponse)
      }

      "submit data  with an invalid response" in new Setup {
        val notFoundResponse: JsValue = Json.parse("""{}""")
        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(NOT_FOUND, notFoundResponse.toString)))

        val result: Future[HttpResponse] = testEtmpConnector.updateRegistrationDetails("SAFE-123", inputJsonForNUK)
        val response: HttpResponse = await(result)
        response.status must be(NOT_FOUND)
      }
    }
  }
}
