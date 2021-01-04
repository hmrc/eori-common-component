/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.services

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.domain.{Journey, RcmNotificationRequest, RecipientDetails}
import uk.gov.hmrc.customs.managesubscription.services.EmailService
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.BaseSpec
import util.TestData.emulatedServiceFailure

import scala.concurrent.Future

class EmailServiceSpec extends BaseSpec {

  private implicit val hc = mock[HeaderCarrier]

  private val mockEmailConnector = mock[EmailConnector]

  private val service = "HMRC-ATAR-ORG"

  private val registerSuccessTemplateId: String     = "customs_registration_successful"
  private val registerNotSuccessTemplateId: String  = "customs_registration_not_successful"
  private val subscribeSuccessTemplateId: String    = "ecc_subscription_successful"
  private val subscribeNotSuccessTemplateId: String = "ecc_subscription_not_successful"
  private val rcmNotificationTemplateId: String     = "ecc_rcm_notifications"

  private lazy val emailService = new EmailService(appConfig, mockEmailConnector)

  private val registerRecipientEmailAddress = "john.doe@example.com"
  private val registerRecipientFullName     = "John Doe"
  private val registerOrgName               = "Test Company Name"
  private val registerServiceName           = "Register Service"
  private val registerCompletionDate        = "5 May 2017"

  private val registerRecipientDetails = RecipientDetails(
    Journey.Register,
    service,
    registerServiceName,
    registerRecipientEmailAddress,
    registerRecipientFullName,
    Some(registerOrgName),
    Some(registerCompletionDate),
    Some("en")
  )

  private val subscribeRecipientEmailAddress = "jane.doe@example.com"
  private val subscribeRecipientFullName     = "Jane Doe"
  private val subscribeOrgName               = "Test Company Name 2"
  private val subscribeServiceName           = "Subscribe Service"
  private val subscribeCompletionDate        = "23 June 2004"

  private val subscribeRecipientDetails = RecipientDetails(
    Journey.Subscribe,
    service,
    subscribeServiceName,
    subscribeRecipientEmailAddress,
    subscribeRecipientFullName,
    Some(subscribeOrgName),
    Some(subscribeCompletionDate),
    Some("en")
  )

  private val rcmNotifyRequest =
    RcmNotificationRequest("a@b.com", "fullname", "GBXXXXXXXXX000", "Some Service", "26-May-2019 08:12:83")

  private val registerSuccessEmail = Email(
    to = List(registerRecipientEmailAddress),
    templateId = registerSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> registerRecipientFullName,
      "recipientOrgName"       -> registerOrgName,
      "serviceName"            -> registerServiceName,
      "completionDate"         -> registerCompletionDate
    )
  )

  private val registerNotSuccessEmail = Email(
    to = List(registerRecipientEmailAddress),
    templateId = registerNotSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> registerRecipientFullName,
      "recipientOrgName"       -> registerOrgName,
      "serviceName"            -> registerServiceName,
      "completionDate"         -> registerCompletionDate
    )
  )

  private val subscribeSuccessEmail = Email(
    to = List(subscribeRecipientEmailAddress),
    templateId = subscribeSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> subscribeRecipientFullName,
      "recipientOrgName"       -> subscribeOrgName,
      "serviceName"            -> subscribeServiceName,
      "completionDate"         -> subscribeCompletionDate
    )
  )

  private val subscribeNotSuccessEmail = Email(
    to = List(subscribeRecipientEmailAddress),
    templateId = subscribeNotSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> subscribeRecipientFullName,
      "recipientOrgName"       -> subscribeOrgName,
      "serviceName"            -> subscribeServiceName,
      "completionDate"         -> subscribeCompletionDate
    )
  )

  private val rcmNotifyEmail = Email(
    to = List("john.doe@example.com"),
    templateId = rcmNotificationTemplateId,
    parameters = Map(
      "email"       -> rcmNotifyRequest.email,
      "name"        -> rcmNotifyRequest.name,
      "eori"        -> rcmNotifyRequest.eori,
      "serviceName" -> rcmNotifyRequest.serviceName,
      "timestamp"   -> rcmNotifyRequest.timestamp
    )
  )

  override def beforeEach() {
    reset(mockEmailConnector)
  }

  "EmailService" should {
    "call emailConnector with proper content for Register success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200, ""))
      )

      emailService.sendSuccessEmail(registerRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(registerSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Register not success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200, ""))
      )

      emailService.sendFailureEmail(registerRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(registerNotSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Subscribe success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200, ""))
      )

      emailService.sendSuccessEmail(subscribeRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(subscribeSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Subscribe not success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200, ""))
      )

      emailService.sendFailureEmail(subscribeRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(subscribeNotSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for RCM notification email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200, ""))
      )

      emailService.sendRcmNotificationEmail(rcmNotifyRequest)

      verify(mockEmailConnector).sendEmail(meq(rcmNotifyEmail))(meq(hc))
    }

    "propagate error when emailConnector fails on sending email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.failed(emulatedServiceFailure)
      )

      the[RuntimeException] thrownBy {
        await(emailService.sendSuccessEmail(registerRecipientDetails))
      } shouldBe emulatedServiceFailure
    }
  }
}
