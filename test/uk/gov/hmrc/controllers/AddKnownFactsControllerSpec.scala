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

package uk.gov.hmrc.controllers

import connectors.GovernmentGatewayAdminConnector
import controllers.AddKnownFactsController
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

class AddKnownFactsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockGGAdminConnector: GovernmentGatewayAdminConnector = mock[GovernmentGatewayAdminConnector]
  val utr = new SaUtrGenerator().nextSaUtr.toString
  val serviceName = "ATED"

  object TestAddKnownFactsController extends AddKnownFactsController {
    val ggAdminConnector: GovernmentGatewayAdminConnector = mockGGAdminConnector
  }

  "AddKnownFactsController" must {

    "use the correct BusinessCustomer connectors" in {
      AddKnownFactsController.ggAdminConnector must be(GovernmentGatewayAdminConnector)
    }

    "known-facts" must {
      val ggSuccess = Json.parse( """{"rowModified":"1"}""")
      val ggSuccessResponse = HttpResponse(OK, responseJson = Some(ggSuccess))
      val ggFailure = Json.parse("""{"error": "some constraint violation"}""")
      val ggFailureResponse = HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(ggFailure))

      "respond with OK for successful add known fact" in {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockGGAdminConnector.addKnownFacts(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(ggSuccessResponse))
        val result = TestAddKnownFactsController.addKnownFacts(utr, "ATED").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(OK)
        contentType(result).get must be("text/plain")
      }

      "for an unsuccessful add known fact call, still return status as OK" in {
        val inputJson = Json.parse(s"""{"serviceName": "$serviceName", "utr": "$utr"}""")
        when(mockGGAdminConnector.addKnownFacts(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(ggFailureResponse))
        val result = TestAddKnownFactsController.addKnownFacts(utr, "ATED").apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(OK)
      }
    }
  }

}
