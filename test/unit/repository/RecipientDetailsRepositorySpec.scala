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

package unit.repository

import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.customs.managesubscription.repository.{RecipientDetailsCacheRepository, RecipientDetailsRepository}
import uk.gov.hmrc.mongo.cache.CacheItem
import util.UnitSpec

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RecipientDetailsRepositorySpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val mockRecipientDetailsCacheRepo = mock[RecipientDetailsCacheRepository]
  implicit val ec: ExecutionContext         = ExecutionContext.Implicits.global

  private val recipientDetailsCache = new RecipientDetailsRepository(mockRecipientDetailsCacheRepo)

  private val idAsString = "some-id"

  private val invalidCacheDataJson =
    Json.parse("""
        |{
        |  "recipientDetailsWithEori": {
        |    "invalid": "invalid"
        |  }
        |}""".stripMargin)

  val recipientDetails: (String, JsValue) =
    "recipientDetailsWithEori" -> invalidCacheDataJson

  "recipient details cache" should {

    "return JsError when invalid Json for recipientDetailsWithEori found" in {
      val invalidCache = CacheItem(idAsString, JsObject.apply(Map(recipientDetails)), Instant.now(), Instant.now())
      when(mockRecipientDetailsCacheRepo.findById(meq(idAsString))).thenReturn(Future.successful(Some(invalidCache)))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.swap.getOrElse("") shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }

    "return JsError when no data found" in {
      when(mockRecipientDetailsCacheRepo.findById(idAsString)).thenReturn(Future.successful(None))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.swap.getOrElse("") shouldBe JsError("No data is saved for the formBundleId: some-id")
    }

    "return error when data not found for recipientDetailsWithEori" in {
      val previousJson =
        Json.parse("""
            |{
            |  "recipientDetails": {
            |    "invalid": "invalid"
            |  }
            |}""".stripMargin)
      val recipientDetails: (String, JsValue) =
        "recipientDetailsWithEori" -> previousJson
      val invalidCache = CacheItem(idAsString, JsObject.apply(Map(recipientDetails)), Instant.now(), Instant.now())

      when(mockRecipientDetailsCacheRepo.findById(idAsString)).thenReturn(Future.successful(Some(invalidCache)))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.swap.getOrElse("") shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }
  }

  override protected def beforeEach(): Unit =
    reset(mockRecipientDetailsCacheRepo)

}
