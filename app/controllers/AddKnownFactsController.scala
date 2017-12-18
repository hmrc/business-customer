/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{GovernmentGatewayAdminConnector, TaxEnrolmentsConnector}
import play.api.Logger
import play.api.mvc.Action
import uk.gov.hmrc.play.microservice.controller.BaseController


import scala.concurrent.ExecutionContext.Implicits.global

trait AddKnownFactsController extends BaseController {

  def ggAdminConnector: GovernmentGatewayAdminConnector
  def taxEnrolmentConnector: TaxEnrolmentsConnector

  def newAddKnownFacts(id: String, serviceName: String, arn: String) = Action.async {
    implicit request =>
      val json = request.body.asJson.get
      taxEnrolmentConnector.addKnownFacts(serviceName, json, arn).map { addKnownFactResponse =>
        addKnownFactResponse.status match {
          case OK => Ok(addKnownFactResponse.body)
          case _ =>
            Logger.warn(s"[AddKnownFactsController][addKnownFacts] - add known fact failed " +
              s"- response.status = ${addKnownFactResponse.status} and response.body = ${addKnownFactResponse.body}")
            Ok(addKnownFactResponse.body)
        }
      }
  }

  def addKnownFacts(id: String, serviceName: String) = Action.async {
    implicit request =>
      val json = request.body.asJson.get
      ggAdminConnector.addKnownFacts(serviceName, json).map { addKnownFactResponse =>
        addKnownFactResponse.status match {
          case OK => Ok(addKnownFactResponse.body)
          case _ =>
            Logger.warn(s"[AddKnownFactsController][addKnownFacts] - add known fact failed " +
              s"- response.status = ${addKnownFactResponse.status} and response.body = ${addKnownFactResponse.body}")
            Ok(addKnownFactResponse.body)
        }
      }
  }

}

object AddKnownFactsController extends AddKnownFactsController {
  val ggAdminConnector: GovernmentGatewayAdminConnector = GovernmentGatewayAdminConnector
  val taxEnrolmentConnector: TaxEnrolmentsConnector = TaxEnrolmentsConnector
}
