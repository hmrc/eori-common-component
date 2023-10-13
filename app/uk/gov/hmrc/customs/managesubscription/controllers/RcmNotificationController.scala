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

import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.customs.managesubscription.controllers.Permissions.internalAuthPermission
import uk.gov.hmrc.customs.managesubscription.domain.RcmNotificationRequest
import uk.gov.hmrc.customs.managesubscription.services.EmailService
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RcmNotificationController @Inject() (
  emailService: EmailService,
  cc: ControllerComponents,
  digitalHeaderValidator: DigitalHeaderValidator,
  val auth: BackendAuthComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def notifyRCM(): Action[RcmNotificationRequest] = digitalHeaderValidator(
    parse.json[RcmNotificationRequest]
  ) andThen auth.authorizedAction(internalAuthPermission("rcm-notification")) async {
    implicit request =>
      emailService.sendRcmNotificationEmail(request.body).map(_ => NoContent)
  }

}
