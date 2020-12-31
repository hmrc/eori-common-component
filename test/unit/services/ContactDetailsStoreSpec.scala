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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsError
import uk.gov.hmrc.customs.managesubscription.domain.RecipientDetailsWithEori
import uk.gov.hmrc.customs.managesubscription.repository.RecipientDetailsRepository
import uk.gov.hmrc.customs.managesubscription.services.RecipientDetailsStore
import util.UnitSpec
import util.TestData._

import scala.concurrent.Future

class ContactDetailsStoreSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val mockContactDetailsRepository = mock[RecipientDetailsRepository]
  private val contactDetailsStore          = new RecipientDetailsStore(mockContactDetailsRepository)
  private val idAsString                   = "some-id"

  "recipient details store" should {

    "return true on saveRecipientDetailsForBundleId when repository has saved" in {
      when(
        mockContactDetailsRepository.saveRecipientDetailsForBundleId(
          idAsString,
          Some(eori),
          recipientDetails,
          emailVerificationTimestamp,
          safeId
        )
      )
        .thenReturn(Future.successful(()))

      val actual = await(
        contactDetailsStore.saveRecipientDetailsForBundleId(
          idAsString,
          Some(eori),
          recipientDetails,
          emailVerificationTimestamp,
          safeId
        )
      )

      actual shouldBe ((): Unit)
    }

    "recipientDetailsForBundleId should return RecipientDetailsWithEori when found by repo" in {
      when(mockContactDetailsRepository.recipientDetailsForBundleId(idAsString)).thenReturn(
        Future.successful(
          Right(RecipientDetailsWithEori(Some(eori.value), recipientDetails, emailVerificationTimestamp, safeId))
        )
      )

      val actual = await(contactDetailsStore.recipientDetailsForBundleId(idAsString))

      actual shouldBe RecipientDetailsWithEori(Some(eori.value), recipientDetails, emailVerificationTimestamp, safeId)
    }

    "recipientDetailsForBundleId should throw exception when recipient details not found in repository" in {
      when(mockContactDetailsRepository.recipientDetailsForBundleId(idAsString)).thenReturn(
        Future.successful(Left(JsError("invalid-json")))
      )

      val caught = intercept[IllegalStateException](await(contactDetailsStore.recipientDetailsForBundleId(idAsString)))

      caught.getMessage contains "invalid-json"
    }
  }

  override protected def beforeEach(): Unit =
    reset(mockContactDetailsRepository)

}
