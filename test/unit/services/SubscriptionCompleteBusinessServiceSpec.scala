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

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.connectors.{CustomsDataStoreConnector, SubscriptionDisplayConnector}
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.SubscriptionCompleteStatus
import uk.gov.hmrc.customs.managesubscription.domain._
import uk.gov.hmrc.customs.managesubscription.services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.SubscriptionResult._
import util.TestData._

import scala.concurrent.Future

class SubscriptionCompleteBusinessServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  private implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val mockContactDetailsStore = mock[RecipientDetailsStore]
  private val mockEmailService = mock[EmailService]
  private val mockAuditable = mock[Auditable]
  private val mockDataStoreConnector = mock[CustomsDataStoreConnector]
  private val mockSubDisplayConnector= mock[SubscriptionDisplayConnector]
  private val service = new SubscriptionCompleteBusinessService(mockContactDetailsStore, mockEmailService, mockAuditable, mockDataStoreConnector, mockSubDisplayConnector)
  private val recipientDetails: RecipientDetails = RecipientDetails("Subscribe","ATar", "john.doe@example.com", "John Doe", Some("Test Company Name"), Some("5 May 2017"))
  private val transactionName = "eori-common-component-update-status"
  private val path = s"/eori-common-component/$formBundleId"
  private val tagsGoodState = Map("state" -> "SUCCEEDED", "formBundleId" -> formBundleId)
  private val tagsBadState = Map("state" -> "EnrolmentError", "formBundleId" -> formBundleId)
  private val auditType = "taxEnrolmentStatus"

  val mockSubscriptionComplete: SubscriptionComplete = mock[SubscriptionComplete]

  before {
    reset(mockContactDetailsStore, mockEmailService, mockSubscriptionComplete, mockAuditable, mockDataStoreConnector, mockSubDisplayConnector)
    when(mockContactDetailsStore.recipientDetailsForBundleId(meq(formBundleId))).thenReturn(Future.successful(RecipientDetailsWithEori(Some(eori.value), recipientDetails, emailVerificationTimestamp, safeId)))
    when(mockSubscriptionComplete.url).thenReturn(url)
  }

  "SubscriptionCompleteBusinessService" should {
    "generate an audit event when subscription completes with a good state" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      when(mockDataStoreConnector.storeEmailAddress(any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockAuditable).sendDataEvent(transactionName, path, tagsGoodState, auditType)
    }

    "generate an audit event when subscription completes with a bad state" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.EnrolmentError)
      doNothing().when(mockAuditable).sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockAuditable).sendDataEvent(transactionName, path, tagsBadState, auditType)
      verifyZeroInteractions(mockEmailService)
    }

    "send success email to recipient on successful SubscriptionComplete" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      when(mockDataStoreConnector.storeEmailAddress(any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockEmailService).sendEmail(recipientDetails, SubscriptionCompleteStatus.SUCCEEDED)(mockHeaderCarrier)
    }

    "send data store request on successful SubscriptionComplete and eori number is available" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      when(mockDataStoreConnector.storeEmailAddress(any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockDataStoreConnector).storeEmailAddress(DataStoreRequest(eori.value, recipientDetails.recipientEmailAddress, emailVerificationTimestamp))(mockHeaderCarrier)
    }

    "send subscription display request when eori number is not found in cache" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockContactDetailsStore.recipientDetailsForBundleId(meq(formBundleId))).thenReturn(Future.successful(RecipientDetailsWithEori(None, recipientDetails, emailVerificationTimestamp, safeId)))
      when(mockSubDisplayConnector.callSubscriptionDisplay(any())(any())).thenReturn(Future.successful(None))
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockSubDisplayConnector).callSubscriptionDisplay(any())(any())
    }

    "not send subscription display request when eori number is found in cache" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      when(mockDataStoreConnector.storeEmailAddress(any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verifyZeroInteractions(mockSubDisplayConnector)
    }

    "not send data store request when eori number is not retrieved from cache or subscription display" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockContactDetailsStore.recipientDetailsForBundleId(meq(formBundleId))).thenReturn(Future.successful(RecipientDetailsWithEori(None, recipientDetails, emailVerificationTimestamp, safeId)))
      when(mockSubDisplayConnector.callSubscriptionDisplay(any())(any())).thenReturn(Future.successful(None))
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verifyZeroInteractions(mockDataStoreConnector)
    }

    "send non-success email to recipient on unsuccessful SubscriptionComplete" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.ERROR)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      when(mockDataStoreConnector.storeEmailAddress(any())(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(200)))
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verify(mockEmailService).sendEmail(recipientDetails, SubscriptionCompleteStatus.ERROR)(mockHeaderCarrier)
    }

    "Do not send any email to recipient or data store request for EnrolmentError" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.EnrolmentError)
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verifyZeroInteractions(mockEmailService)
      verifyZeroInteractions(mockDataStoreConnector)
    }

    "Do not send any email to recipient or data store request for AuthRefreshed" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.AuthRefreshed)
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verifyZeroInteractions(mockEmailService)
      verifyZeroInteractions(mockDataStoreConnector)
    }

    "Do not send any email to recipient or data store request for Enrolled" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.Enrolled)
      await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      verifyZeroInteractions(mockEmailService)
      verifyZeroInteractions(mockDataStoreConnector)
    }

    "propagate error when email service fails" in {
      mockSubscriptionComplete(SubscriptionCompleteStatus.SUCCEEDED)
      when(mockEmailService.sendEmail(any(), any())(any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

      intercept[RuntimeException] {
        await(service.onSubscriptionStatus(mockSubscriptionComplete, formBundleId))
      } shouldBe emulatedServiceFailure
    }
  }

  private def mockSubscriptionComplete(status: SubscriptionCompleteStatus) {
    when(mockSubscriptionComplete.state).thenReturn(status)
  }
}
