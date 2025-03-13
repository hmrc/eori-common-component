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

package unit.services

import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import play.api.test.Helpers._
import uk.gov.hmrc.customs.managesubscription.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.customs.managesubscription.domain.protocol.{Eori, TaxEnrolmentsRequest}
import uk.gov.hmrc.customs.managesubscription.domain.{RecipientDetails, TaxPayerId}
import uk.gov.hmrc.customs.managesubscription.services.{RecipientDetailsStore, TaxEnrolmentsService}
import uk.gov.hmrc.http.HeaderCarrier
import util.BaseSpec
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsServiceSpec extends BaseSpec {

  private val mockContactDetailsStore   = mock[RecipientDetailsStore]
  private val mockTaxEnrolmentConnector = mock[TaxEnrolmentsConnector]

  private val service = new TaxEnrolmentsService(appConfig, mockTaxEnrolmentConnector, mockContactDetailsStore)

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val formBundleId: String = "Form-Bundle-Id-123"
  private val taxPayerId           = TaxPayerId("sap-number")
  private val eori                 = Eori("GB0123456789")

  private val taxEnrolmentRequest = TaxEnrolmentsRequest(
    recipientDetails.enrolmentKey,
    appConfig.taxEnrolmentsCallbackUrl + "/" + formBundleId,
    etmpId = taxPayerId.id
  )

  private val emulatedFailure = new UnsupportedOperationException("Emulated service call failure.")

  override protected def beforeEach(): Unit =
    reset(mockContactDetailsStore, mockTaxEnrolmentConnector)

  "SubscriptionTaxEnrolmentIntegrationService" should {

    "store recipient details in store and call tax-enrolments connector" in {
      when(
        mockContactDetailsStore.saveRecipientDetailsForBundleId(
          anyString,
          any[Option[Eori]],
          any[RecipientDetails],
          anyString,
          anyString
        )
      ).thenReturn(Future.unit)
      when(mockTaxEnrolmentConnector.enrol(any[TaxEnrolmentsRequest], anyString)(any[HeaderCarrier])).thenReturn(
        Future.successful(NO_CONTENT)
      )

      await(
        service.saveRecipientDetailsAndCallTaxEnrolment(
          formBundleId,
          recipientDetails,
          sapNumber = taxPayerId,
          Some(eori),
          emailVerificationTimestamp,
          safeId
        )
      )

      verify(mockContactDetailsStore).saveRecipientDetailsForBundleId(
        meq(formBundleId),
        meq(Some(eori)),
        meq(recipientDetails),
        meq(emailVerificationTimestamp),
        meq(safeId)
      )
      verify(mockTaxEnrolmentConnector).enrol(meq(taxEnrolmentRequest), meq(formBundleId))(meq(headerCarrier))
    }

    "save recipient details in store and call tax-enrolments connector when pending request from frontend" in {
      when(
        mockContactDetailsStore.saveRecipientDetailsForBundleId(
          anyString,
          any[Option[Eori]],
          any[RecipientDetails],
          anyString,
          anyString
        )
      ).thenReturn(Future.unit)
      when(mockTaxEnrolmentConnector.enrol(any[TaxEnrolmentsRequest], anyString)(any[HeaderCarrier])).thenReturn(
        Future.successful(NO_CONTENT)
      )

      await(
        service.saveRecipientDetailsAndCallTaxEnrolment(
          formBundleId,
          recipientDetails,
          sapNumber = taxPayerId,
          None,
          emailVerificationTimestamp,
          safeId
        )
      ) shouldBe NO_CONTENT

      verify(mockContactDetailsStore).saveRecipientDetailsForBundleId(
        meq(formBundleId),
        meq(None),
        meq(recipientDetails),
        meq(emailVerificationTimestamp),
        meq(safeId)
      )
      verify(mockTaxEnrolmentConnector).enrol(meq(taxEnrolmentRequest), meq(formBundleId))(meq(headerCarrier))
    }

    "fail when storing recipient details fails" in {
      when(
        mockContactDetailsStore.saveRecipientDetailsForBundleId(
          anyString,
          any[Option[Eori]],
          any[RecipientDetails],
          anyString,
          anyString
        )
      ).thenReturn(Future.failed(emulatedFailure))

      the[UnsupportedOperationException] thrownBy {
        await(
          service.saveRecipientDetailsAndCallTaxEnrolment(
            formBundleId,
            recipientDetails,
            sapNumber = taxPayerId,
            Some(eori),
            emailVerificationTimestamp,
            safeId
          )
        )
      } shouldBe emulatedFailure
      verifyZeroInteractions(mockTaxEnrolmentConnector)
    }

    "fail when calling tax-enrolments fails" in {
      when(
        mockContactDetailsStore.saveRecipientDetailsForBundleId(
          anyString,
          any[Option[Eori]],
          any[RecipientDetails],
          anyString,
          anyString
        )
      ).thenReturn(Future.unit)

      when(mockTaxEnrolmentConnector.enrol(any[TaxEnrolmentsRequest], anyString)(any[HeaderCarrier])).thenReturn(
        Future.failed(emulatedFailure)
      )

      the[UnsupportedOperationException] thrownBy {
        await(
          service.saveRecipientDetailsAndCallTaxEnrolment(
            formBundleId,
            recipientDetails,
            sapNumber = taxPayerId,
            Some(eori),
            emailVerificationTimestamp,
            safeId
          )
        )
      } shouldBe emulatedFailure
    }
  }
}
