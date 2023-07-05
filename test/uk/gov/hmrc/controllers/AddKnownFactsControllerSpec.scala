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

package uk.gov.hmrc.controllers

import connectors.{GovernmentGatewayAdminConnector, TaxEnrolmentsConnector}
import controllers.AddKnownFactsController
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

class AddKnownFactsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  val mockGGAdminConnector: GovernmentGatewayAdminConnector = mock[GovernmentGatewayAdminConnector]
  val mockTaxEnrolmentConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
  val utr = new SaUtrGenerator().nextSaUtr.toString
  val serviceName = "ATED"

  trait Setup {
    val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

    class TestAddKnownFactsController extends BackendController(cc) with AddKnownFactsController {
      val ggAdminConnector: GovernmentGatewayAdminConnector = mockGGAdminConnector
      val taxEnrolmentConnector: TaxEnrolmentsConnector = mockTaxEnrolmentConnector
    }

    val controller = new TestAddKnownFactsController()
  }


  "AddKnownFactsController" must {

    "known-facts" must {
      val ggSuccess = Json.parse( """{"rowModified":"1"}""")
      val ggSuccessResponse = HttpResponse(NO_CONTENT, ggSuccess.toString)
      val ggFailure = Json.parse("""{"error": "some constraint violation"}""")
      val ggFailureResponse = HttpResponse(INTERNAL_SERVER_ERROR, ggFailure.toString)

      "respond with OK for successful add known fact" in new Setup {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockGGAdminConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(ggSuccessResponse))
        val result = controller.addKnownFacts(utr, "ATED").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(NO_CONTENT)
      }

      "for an unsuccessful add known fact call, still return status as OK" in new Setup {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockGGAdminConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(ggFailureResponse))
        val result = controller.addKnownFacts(utr, "ATED").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(OK)
      }
    }

    "add new known-facts" must {
      val ggSuccess = Json.parse( """{"rowModified":"1"}""")
      val ggSuccessResponse = HttpResponse(NO_CONTENT, ggSuccess.toString)
      val ggFailure = Json.parse("""{"error": "some constraint violation"}""")
      val ggFailureResponse = HttpResponse(INTERNAL_SERVER_ERROR, ggFailure.toString)

      "respond with OK for successful add known fact" in new Setup {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockTaxEnrolmentConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(ggSuccessResponse))
        val result = controller.newAddKnownFacts(utr, "ATED", "JARN123456").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(NO_CONTENT)
      }

      "for an unsuccessful add known fact call, still return status as OK" in new Setup {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockTaxEnrolmentConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(ggFailureResponse))
        val result = controller.newAddKnownFacts(utr, "ATED", "JARN123456").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(OK)
      }
    }
  }

}
