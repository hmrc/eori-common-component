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

import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.customs.managesubscription.connectors.{CustomsDataStoreConnector, MicroserviceAuthConnector}
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDataStoreController @Inject() (
  customsDataStore: CustomsDataStoreConnector,
  cc: ControllerComponents,
  override val authConnector: MicroserviceAuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthorisedFunctions {

  def updateCustomsDataStore(): Action[AnyContent] = Action async {
    implicit request =>
      authorised(AuthProviders(GovernmentGateway)) {
        request.body.asJson.fold(ifEmpty = Future.successful(ErrorResponse.ErrorGenericBadRequest.JsonResult)) { js =>
          js.validate[DataStoreRequest] match {
            case JsSuccess(r, _) =>
              customsDataStore.updateDataStore(r).map(_ => NoContent)
            case JsError(_) =>
              Future.successful(ErrorResponse.ErrorInvalidPayload.JsonResult)
          }
        }
      }
  }

}
