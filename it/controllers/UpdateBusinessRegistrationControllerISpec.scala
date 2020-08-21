package controllers

import helpers.IntegrationSpec
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import com.github.tomakehurst.wiremock.client.WireMock._

class UpdateBusinessRegistrationControllerISpec extends IntegrationSpec {

  Map(
    "org"   -> "orgName",
    "sa"    -> "userName",
    "agent" -> "agentName"
  ) foreach { case (userType, name) =>
    s"/$userType/$name/business-customer/update/$userType" should {
      "lookup business details" when {
        "business details are available" in {

          stubFor(put(urlMatching(s"/registration/safeid/$userType"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(
                  s"""{
                     |}""".stripMargin
                )
            )
          )

          val result: WSResponse = await(hitApplicationEndpoint(s"/$userType/$name/business-customer/update/$userType")
            .post(Json.parse(s"""
             |{
             |}
            """.stripMargin)))

          result.status mustBe 200
        }
      }
    }
  }
}