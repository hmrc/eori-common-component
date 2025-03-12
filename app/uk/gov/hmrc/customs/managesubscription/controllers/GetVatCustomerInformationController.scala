/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Results}
import uk.gov.hmrc.customs.managesubscription.controllers.Permissions.internalAuthPermission
import uk.gov.hmrc.customs.managesubscription.services.GetVatCustomerInformationService
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class GetVatCustomerInformationController @Inject() (
  getVatCustomerInformationService: GetVatCustomerInformationService,
  auth: BackendAuthComponents,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getVatCustomerInformation(vrn: String): Action[AnyContent] =
    auth.authorizedAction(internalAuthPermission("vat")).async {
      getVatCustomerInformationService.getVatCustomerInformation(vrn).fold(
        responseError => Results.Status(responseError.status)(Json.toJson(responseError)).as(MimeTypes.JSON),
        vatInfo => Ok(Json.toJson(vatInfo.toResponse))
      )
    }

}
