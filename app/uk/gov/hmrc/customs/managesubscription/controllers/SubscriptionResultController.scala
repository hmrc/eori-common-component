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

package uk.gov.hmrc.customs.managesubscription.controllers

import play.api.i18n.Lang.logger
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.managesubscription.controllers.json.JsonReads._
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionComplete
import uk.gov.hmrc.customs.managesubscription.services.SubscriptionCompleteBusinessService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionResultController @Inject() (
  subscriptionCompleteBusinessService: SubscriptionCompleteBusinessService,
  cc: ControllerComponents,
  messagingHeaderValidator: MessagingHeaderValidator
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def updateStatus(formBundleId: String): Action[AnyContent] = messagingHeaderValidator.async { implicit request =>
    request.body.asJson.fold(ifEmpty = Future.successful(ErrorResponse.ErrorGenericBadRequest.JsonResult)) { js =>
      js.validate[SubscriptionComplete] match {
        case JsSuccess(subscriptionComplete, _) =>
          subscriptionCompleteBusinessService.onSubscriptionStatus(subscriptionComplete, formBundleId).map(
            _ => NoContent
          )
        case JsError(e) =>
          logger.error(s"Received invalid SubscriptionComplete. Validation errors: ${e.mkString}")
          Future.successful(ErrorResponse.ErrorInvalidPayload.JsonResult)
      }
    }
  }

}
