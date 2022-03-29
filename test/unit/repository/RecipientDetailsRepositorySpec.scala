/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import uk.gov.hmrc.customs.managesubscription.repository.{RecipientDetailsCacheRepository, RecipientDetailsRepository}
import uk.gov.hmrc.mongo.cache.CacheItem
import util.UnitSpec

import java.time.Instant
import scala.concurrent.Future

class RecipientDetailsRepositorySpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val mockRecipientDetailsCacheRepo = mock[RecipientDetailsCacheRepository]

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

      result.left.get shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }

    "return JsError when no data found" in {
      val invalidCache = CacheItem(idAsString, JsObject.empty, Instant.now(), Instant.now())
      when(mockRecipientDetailsCacheRepo.findById(idAsString)).thenReturn(Future.successful(None))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.left.get shouldBe JsError("No data is saved for the formBundleId: some-id")
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

      result.left.get shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }
  }

  override protected def beforeEach(): Unit =
    reset(mockRecipientDetailsCacheRepo)

}
