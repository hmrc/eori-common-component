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

package uk.gov.hmrc.customs.managesubscription.services

import com.google.inject.Singleton
import javax.inject.Inject
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.domain.RecipientDetails
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.{SubscriptionCompleteStatus, _}
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class EmailService @Inject()(appConfig: AppConfig, emailConnector: EmailConnector) {
  private val Register = "Register"
  private val Subscribe = "Subscribe"

  // TODO match may not be exhaustive - add default case just to clear compilation warning
  def sendEmail(recipient: RecipientDetails, status: SubscriptionCompleteStatus)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    (recipient.journey, status) match {
      case (Register, SUCCEEDED) => sendEmail(appConfig.emailGyeSuccessTemplateId, recipient)
      case (Register, ERROR) => sendEmail(appConfig.emailGyeNotSuccessTemplateId, recipient)
      case (Subscribe, SUCCEEDED) => sendEmail(appConfig.emailMigrateSuccessTemplateId, recipient)
      case (Subscribe, ERROR) => sendEmail(appConfig.emailMigrateNotSuccessTemplateId, recipient)
    }

  private def sendEmail(templateId: String, recipient: RecipientDetails)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val email = Email(
      to = List(recipient.recipientEmailAddress),
      templateId = templateId,
      parameters = Map(
        "recipientName_FullName" -> recipient.recipientFullName,
        "recipientOrgName" -> recipient.orgName.getOrElse(""),
        "completionDate" -> recipient.completionDate.getOrElse("")
      )
    )
    emailConnector.sendEmail(email)
  }
}
