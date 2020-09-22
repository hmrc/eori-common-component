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

package unit.services

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import uk.gov.hmrc.customs.managesubscription.connectors.EmailConnector
import uk.gov.hmrc.customs.managesubscription.domain.{Journey, RecipientDetails, SubscriptionCompleteStatus}
import uk.gov.hmrc.customs.managesubscription.services.EmailService
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.BaseSpec
import util.TestData.emulatedServiceFailure

import scala.concurrent.Future

class EmailServiceSpec extends BaseSpec {

  private implicit val hc = mock[HeaderCarrier]

  private val mockEmailConnector = mock[EmailConnector]

  private val gyeSuccessTemplateId: String        = "customs_registration_successful"
  private val gyeNotSuccessTemplateId: String     = "customs_registration_not_successful"
  private val migrateSuccessTemplateId: String    = "customs_migrate_successful"
  private val migrateNotSuccessTemplateId: String = "customs_migrate_not_successful"

  private lazy val emailService = new EmailService(appConfig, mockEmailConnector)

  private val gyeRecipientEmailAddress = "john.doe@example.com"
  private val gyeRecipientFullName     = "John Doe"
  private val gyeOrgName               = "Test Company Name"
  private val gyeCompletionDate        = "5 May 2017"

  private val gyeRecipientDetails = RecipientDetails(
    Journey.Register,
    "ATaR",
    gyeRecipientEmailAddress,
    gyeRecipientFullName,
    Some(gyeOrgName),
    Some(gyeCompletionDate)
  )

  private val migrateRecipientEmailAddress = "jane.doe@example.com"
  private val migrateRecipientFullName     = "Jane Doe"
  private val migrateOrgName               = "Test Company Name 2"
  private val migrateCompletionDate        = "23 June 2004"

  private val migrateRecipientDetails = RecipientDetails(
    Journey.Subscribe,
    "ATaR",
    migrateRecipientEmailAddress,
    migrateRecipientFullName,
    Some(migrateOrgName),
    Some(migrateCompletionDate)
  )

  private val getYourEORISuccessEmail = Email(
    to = List(gyeRecipientEmailAddress),
    templateId = gyeSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> gyeRecipientFullName,
      "recipientOrgName"       -> gyeOrgName,
      "completionDate"         -> gyeCompletionDate
    )
  )

  private val getYourEORINotSuccessEmail = Email(
    to = List(gyeRecipientEmailAddress),
    templateId = gyeNotSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> gyeRecipientFullName,
      "recipientOrgName"       -> gyeOrgName,
      "completionDate"         -> gyeCompletionDate
    )
  )

  private val migrateSuccessEmail = Email(
    to = List(migrateRecipientEmailAddress),
    templateId = migrateSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> migrateRecipientFullName,
      "recipientOrgName"       -> migrateOrgName,
      "completionDate"         -> migrateCompletionDate
    )
  )

  private val migrateNotSuccessEmail = Email(
    to = List(migrateRecipientEmailAddress),
    templateId = migrateNotSuccessTemplateId,
    parameters = Map(
      "recipientName_FullName" -> migrateRecipientFullName,
      "recipientOrgName"       -> migrateOrgName,
      "completionDate"         -> migrateCompletionDate
    )
  )

  override def beforeEach() {
    reset(mockEmailConnector)
  }

  "EmailService" should {
    "call emailConnector with proper content for Register success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200))
      )

      emailService.sendSuccessEmail(gyeRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(getYourEORISuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Register not success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200))
      )

      emailService.sendFailureEmail(gyeRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(getYourEORINotSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Subscribe success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200))
      )

      emailService.sendSuccessEmail(migrateRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(migrateSuccessEmail))(meq(hc))
    }

    "call emailConnector with proper content for Subscribe not success email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(200))
      )

      emailService.sendFailureEmail(migrateRecipientDetails)

      verify(mockEmailConnector).sendEmail(meq(migrateNotSuccessEmail))(meq(hc))
    }

    "propagate error when emailConnector fails on sending email" in {
      when(mockEmailConnector.sendEmail(any[Email])(any[HeaderCarrier])).thenReturn(
        Future.failed(emulatedServiceFailure)
      )

      the[RuntimeException] thrownBy {
        await(emailService.sendSuccessEmail(gyeRecipientDetails))
      } shouldBe emulatedServiceFailure
    }
  }
}
