/*
 * Copyright 2026 HM Revenue & Customs
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

package integration

import base.IntegrationTestsWithDbSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.await
import uk.gov.hmrc.customs.managesubscription.domain.RecipientDetailsWithEori
import uk.gov.hmrc.customs.managesubscription.repository.{RecipientDetailsCacheRepository, RecipientDetailsRepository}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.TestData.*

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class RecipientDetailsRepositoryIntegrationSpec
    extends IntegrationTestsWithDbSpec with MockitoSugar with MongoSupport with ScalaFutures {

  val mockTimeStampSupport = new CurrentTimestampSupport()
  val mockServiceConfig    = mock[ServicesConfig]
  when(mockServiceConfig.getDuration(any[String])).thenReturn(Duration(5000, TimeUnit.SECONDS))

  val repository                                             = new RecipientDetailsCacheRepository(mockServiceConfig, mongoComponent, mockTimeStampSupport)
  val recipientDetailsRepository: RecipientDetailsRepository = new RecipientDetailsRepository(repository)

  "recipient details repository" should {

    "store, fetch and update Recipient Details correctly" in {
      val formBundleId = UUID.randomUUID().toString

      await(
        recipientDetailsRepository.saveRecipientDetailsForBundleId(
          formBundleId,
          Some(eori),
          recipientDetails,
          emailVerificationTimestamp,
          safeId
        )
      )

      val cacheItem =
        repository.findById(formBundleId).futureValue.getOrElse(throw new IllegalStateException("Cache returned None"))

      val cacheValue = cacheItem.data
      (cacheValue \ "recipientDetailsWithEori").as[RecipientDetailsWithEori] shouldBe RecipientDetailsWithEori(
        Some(eori.value),
        recipientDetails,
        emailVerificationTimestamp,
        safeId
      )

      recipientDetailsRepository.recipientDetailsForBundleId(formBundleId).futureValue shouldBe Right(
        RecipientDetailsWithEori(Some(eori.value), recipientDetails, emailVerificationTimestamp, safeId)
      )

      val updatedRecipientDetails = recipientDetails.copy(recipientFullName = "new name")

      await(
        recipientDetailsRepository.saveRecipientDetailsForBundleId(
          formBundleId,
          Some(eori),
          updatedRecipientDetails,
          emailVerificationTimestamp,
          safeId
        )
      )

      val updatedCacheItem =
        repository.findById(formBundleId).futureValue.getOrElse(throw new IllegalStateException("Cache returned None"))
      val updatedCacheValue = updatedCacheItem.data

      (updatedCacheValue \ "recipientDetailsWithEori").as[RecipientDetailsWithEori] shouldBe RecipientDetailsWithEori(
        Some(eori.value),
        updatedRecipientDetails,
        emailVerificationTimestamp,
        safeId
      )
    }

    "expect no data when Recipient Details not available in cache" in {
      val result = recipientDetailsRepository.recipientDetailsForBundleId("not-there").futureValue
      result.swap.getOrElse("").toString should include("No data is saved for the formBundleId: not-there")
    }
  }
}
