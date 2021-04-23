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

package uk.gov.hmrc.controllers

import connectors.EtmpConnector
import controllers.UpdateBusinessRegistrationController
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

class UpdateBusinessRegistrationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  val mockDesConnector: EtmpConnector = mock[EtmpConnector]
  val utr = "testUtr"

  trait Setup {
    val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

    class TestUpdateBusinessRegistrationController extends BackendController(cc) with UpdateBusinessRegistrationController {
      val desConnector: EtmpConnector = mockDesConnector
    }

    val controller: TestUpdateBusinessRegistrationController = new TestUpdateBusinessRegistrationController
  }

  "UpdateBusinessRegistrationController" must {

    "updateRegistration" must {
      val successResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z","sapNumber":"sapNumber","safeId":"XE000123456789","agentReferenceNumber":"01234567890"}""")

      val registerSuccessResponse = HttpResponse(OK, successResponse.toString)
      val matchFailure = Json.parse("""{"reason": "Resource not found"}""")
      val matchFailureResponse = HttpResponse(NOT_FOUND, matchFailure.toString)

      val safeId = "XE000123456789"
      "respond with OK" in new Setup {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        when(mockDesConnector.updateRegistrationDetails(ArgumentMatchers.eq(safeId), ArgumentMatchers.any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = controller.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(OK)
        contentType(result).get must be("text/plain")
        contentAsJson(result) must be(successResponse)
      }

      "for an unsuccessful match return Not found" in new Setup {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        when(mockDesConnector.updateRegistrationDetails(ArgumentMatchers.eq(safeId), ArgumentMatchers.any())).thenReturn(Future.successful(matchFailureResponse))
        val result = controller.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(NOT_FOUND)
        contentAsJson(result) must be(matchFailureResponse.json)
      }

      "for a bad request, return BadRequest" in new Setup {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val badRequestJson = Json.parse("""{"reason" : "Bad Request"}""")
        when(mockDesConnector.updateRegistrationDetails(ArgumentMatchers.eq(safeId), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestJson.toString)))
        val result = controller.update(utr, safeId)(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(badRequestJson)
      }

      "for service unavailable, return service unavailable" in new Setup {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val serviceUnavailable = Json.parse("""{"reason" : "Service unavailable"}""")
        when(mockDesConnector.updateRegistrationDetails(ArgumentMatchers.eq(safeId), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, serviceUnavailable.toString)))
        val result = controller.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(SERVICE_UNAVAILABLE)
        contentAsJson(result) must be(serviceUnavailable)
      }

      "internal server error, return internal server error" in new Setup {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val serverError = Json.parse("""{"reason" : "Internal server error"}""")
        when(mockDesConnector.updateRegistrationDetails(ArgumentMatchers.eq(safeId), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, serverError.toString)))
        val result = controller.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(INTERNAL_SERVER_ERROR)
        contentAsJson(result) must be(serverError)
      }
    }
  }

}
