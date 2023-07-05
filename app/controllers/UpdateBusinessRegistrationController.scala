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

package controllers

import connectors.EtmpConnector
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DefaultUpdateBusinessRegistrationController @Inject()(
                                                             cc: ControllerComponents,
                                                             val desConnector: EtmpConnector
                                                           ) extends BackendController(cc) with UpdateBusinessRegistrationController

@Singleton
class SaUpdateBusinessRegistrationController @Inject()(
                                                        cc: ControllerComponents,
                                                        val desConnector: EtmpConnector
                                                      ) extends BackendController(cc) with UpdateBusinessRegistrationController

@Singleton
class AgentUpdateBusinessRegistrationController @Inject()(
                                                           cc: ControllerComponents,
                                                           val desConnector: EtmpConnector
                                                         )extends BackendController(cc) with UpdateBusinessRegistrationController

trait UpdateBusinessRegistrationController extends BackendController {

  def desConnector: EtmpConnector

  def update(utr: String, safeId: String): Action[AnyContent] = Action.async {
    implicit request =>
      val json = request.body.asJson.get
      desConnector.updateRegistrationDetails(safeId, json).map { updateResponse =>
        updateResponse.status match {
          case OK => Ok(updateResponse.body)
          case NOT_FOUND => NotFound(updateResponse.body)
          case BAD_REQUEST => BadRequest(updateResponse.body)
          case SERVICE_UNAVAILABLE => ServiceUnavailable(updateResponse.body)
          case INTERNAL_SERVER_ERROR | _ => InternalServerError(updateResponse.body)
        }
      }
  }

}
