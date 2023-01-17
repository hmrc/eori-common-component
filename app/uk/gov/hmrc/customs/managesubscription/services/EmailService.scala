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

package uk.gov.hmrc.customs.managesubscription.services

import com.google.inject.Singleton
import javax.inject.Inject
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.domain.{Journey, RcmNotificationRequest, RecipientDetails}
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class EmailService @Inject() (appConfig: AppConfig, emailConnector: EmailConnector) {

  def sendSuccessEmail(recipient: RecipientDetails)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    recipient.journey match {
      case Journey.Register  => sendEmail(appConfig.emailRegisterSuccessTemplateId, recipient)
      case Journey.Subscribe => sendEmail(appConfig.emailSubscribeSuccessTemplateId, recipient)
    }

  def sendFailureEmail(recipient: RecipientDetails)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    recipient.journey match {
      case Journey.Register  => sendEmail(appConfig.emailRegisterNotSuccessTemplateId, recipient)
      case Journey.Subscribe => sendEmail(appConfig.emailSubscribeNotSuccessTemplateId, recipient)
    }

  def sendRcmNotificationEmail(request: RcmNotificationRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val templateId = appConfig.emailRCMTemplateId
    val rcmEmail   = appConfig.rcmEmailAddress.split(",").map(_.trim).toList
    val email      = Email(to = rcmEmail, templateId = templateId, parameters = request.toMap)
    emailConnector.sendEmail(email)
  }

  private def sendEmail(templateId: String, recipient: RecipientDetails)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val email = Email(
      to = List(recipient.recipientEmailAddress),
      templateId = if (recipient.languageCode.contains("cy")) s"${templateId}_cy" else templateId,
      parameters = Map(
        "recipientName_FullName" -> recipient.recipientFullName,
        "recipientOrgName"       -> recipient.orgName.getOrElse(""),
        "serviceName"            -> recipient.serviceName,
        "completionDate"         -> recipient.completionDate.getOrElse("")
      )
    )
    emailConnector.sendEmail(email)
  }

}
