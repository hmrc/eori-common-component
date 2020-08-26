/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customs.managesubscription.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.managesubscription.controllers.json.JsonReads._
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionComplete
import uk.gov.hmrc.customs.managesubscription.services.SubscriptionCompleteBusinessService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionResultController @Inject()(
  subscriptionCompleteBusinessService: SubscriptionCompleteBusinessService,
  cc: ControllerComponents,
  messagingHeaderValidator: MessagingHeaderValidator
) extends BackendController(cc) {

def updateStatus(formBundleId: String): Action[AnyContent] = messagingHeaderValidator.async { implicit request =>
    request.body.asJson.fold(ifEmpty = Future.successful(ErrorResponse.ErrorGenericBadRequest.JsonResult)) { js =>
      js.validate[SubscriptionComplete] match {
        case JsSuccess(subscriptionComplete, _) =>
          subscriptionCompleteBusinessService.onSubscriptionStatus(subscriptionComplete, formBundleId).map(_ => NoContent)
        case _ => Future.successful(ErrorResponse.ErrorInvalidPayload.JsonResult)
      }
    }
  }
}

