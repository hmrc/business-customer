/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import connectors.EtmpConnector
import play.api.Logger
import play.api.mvc.Action
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

trait BusinessRegistrationController extends BaseController {

  def desConnector: EtmpConnector

  def register(id: String) = Action.async {
    implicit request =>
      val json = request.body.asJson.get
      desConnector.register(json).map { registerResponse =>
        registerResponse.status match {
          case OK => Ok(registerResponse.body)
          case NOT_FOUND => NotFound(registerResponse.body)
          case BAD_REQUEST => BadRequest(registerResponse.body)
          case SERVICE_UNAVAILABLE => ServiceUnavailable(registerResponse.body)
          case INTERNAL_SERVER_ERROR | _ => InternalServerError(registerResponse.body)
        }
      }
  }

}

object BusinessRegistrationController extends BusinessRegistrationController {
  val desConnector: EtmpConnector = EtmpConnector
}

object SaBusinessRegistrationController extends BusinessRegistrationController {
  val desConnector: EtmpConnector = EtmpConnector
}

object AgentBusinessRegistrationController extends BusinessRegistrationController {
  val desConnector: EtmpConnector = EtmpConnector
}
