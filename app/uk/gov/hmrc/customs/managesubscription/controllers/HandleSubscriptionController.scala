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
import uk.gov.hmrc.customs.managesubscription.connectors.HttpStatusCheck
import uk.gov.hmrc.customs.managesubscription.domain.protocol.Eori
import uk.gov.hmrc.customs.managesubscription.domain.{HandleSubscriptionRequest, TaxPayerId}
import uk.gov.hmrc.customs.managesubscription.services.TaxEnrolmentsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HandleSubscriptionController @Inject() (
  taxEnrolmentsService: TaxEnrolmentsService,
  cc: ControllerComponents,
  digitalHeaderValidator: DigitalHeaderValidator,
  override val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions {

  def handle(): Action[AnyContent] =
    digitalHeaderValidator async {
      implicit request =>
        authorised() {
          request.body.asJson.fold(ifEmpty = Future.successful(ErrorResponse.ErrorGenericBadRequest.JsonResult)) { js =>
            js.validate[HandleSubscriptionRequest] match {
              case JsSuccess(subscriptionRequest, _) =>
                taxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(
                  subscriptionRequest.formBundleId,
                  subscriptionRequest.recipientDetails,
                  TaxPayerId(subscriptionRequest.sapNumber),
                  subscriptionRequest.eori.map(Eori(_)),
                  subscriptionRequest.emailVerificationTimestamp,
                  subscriptionRequest.safeId
                ).map(status => if (HttpStatusCheck.is2xx(status)) NoContent else InternalServerError)
              case JsError(e) =>
                logger.error(s"Received invalid HandleSubscriptionRequest. Validation errors: ${e.mkString}")
                Future.successful(ErrorResponse.ErrorInvalidPayload.JsonResult)

            }
          }
        }
    }

}
