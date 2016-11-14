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

import connectors.EtmpConnector
import controllers.{AgentUpdateBusinessRegistrationController, UpdateBusinessRegistrationController, SaUpdateBusinessRegistrationController}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

class UpdateBusinessRegistrationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockDesConnector: EtmpConnector = mock[EtmpConnector]
  val utr = "testUtr"

  object TestUpdateBusinessRegistrationController extends UpdateBusinessRegistrationController {
    val desConnector: EtmpConnector = mockDesConnector
  }

  "UpdateBusinessRegistrationController" must {

    "use the correct BusinessCustomer connector" in {
      UpdateBusinessRegistrationController.desConnector must be(EtmpConnector)
      SaUpdateBusinessRegistrationController.desConnector must be(EtmpConnector)
      AgentUpdateBusinessRegistrationController.desConnector must be(EtmpConnector)
    }


    "updateRegistration" must {
      val successResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z","sapNumber":"sapNumber","safeId":"XE000123456789","agentReferenceNumber":"01234567890"}""")

      val registerSuccessResponse = HttpResponse(OK, responseJson = Some(successResponse))
      val matchFailure = Json.parse("""{"reason": "Resource not found"}""")
      val matchFailureResponse = HttpResponse(NOT_FOUND, responseJson = Some(matchFailure))

      val safeId = "XE000123456789"
      "respond with OK" in {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        when(mockDesConnector.updateRegistrationDetails(Matchers.eq(safeId), Matchers.any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestUpdateBusinessRegistrationController.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(OK)
        contentType(result).get must be("text/plain")
        contentAsJson(result) must be(successResponse)
      }

      "for an unsuccessful match return Not found" in {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        when(mockDesConnector.updateRegistrationDetails(Matchers.eq(safeId), Matchers.any())).thenReturn(Future.successful(matchFailureResponse))
        val result = TestUpdateBusinessRegistrationController.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(NOT_FOUND)
        contentAsJson(result) must be(matchFailureResponse.json)
      }

      "for a bad request, return BadRequest" in {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val badRequestJson = Json.parse("""{"reason" : "Bad Request"}""")
        when(mockDesConnector.updateRegistrationDetails(Matchers.eq(safeId), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson))))
        val result = TestUpdateBusinessRegistrationController.update(utr, safeId)(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(badRequestJson)
      }

      "for service unavailable, return service unavailable" in {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val serviceUnavailable = Json.parse("""{"reason" : "Service unavailable"}""")
        when(mockDesConnector.updateRegistrationDetails(Matchers.eq(safeId), Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable))))
        val result = TestUpdateBusinessRegistrationController.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(SERVICE_UNAVAILABLE)
        contentAsJson(result) must be(serviceUnavailable)
      }

      "internal server error, return internal server error" in {
        val inputJsonForNUK = Json.parse("""{"acknowledgementReference":"session-ea091388-d834-4b34-8b8a-caa396e2636a","organisation":{"organisationName":"ACME"},"address":{"addressLine1":"111","addressLine2":"ABC Street","addressLine3":"ABC city","addressLine4":"ABC 123","countryCode":"UK"},"isAnAgent":false,"isAGroup":false,"nonUKIdentification":{"idNumber":"id1","issuingInstitution":"HMRC","issuingCountryCode":"UK"}}""")
        val serverError = Json.parse("""{"reason" : "Internal server error"}""")
        when(mockDesConnector.updateRegistrationDetails(Matchers.eq(safeId), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError))))
        val result = TestUpdateBusinessRegistrationController.update(utr, safeId).apply(FakeRequest().withJsonBody(inputJsonForNUK))
        status(result) must be(INTERNAL_SERVER_ERROR)
        contentAsJson(result) must be(serverError)
      }
    }
  }

}
