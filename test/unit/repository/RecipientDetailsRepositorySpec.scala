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

package unit.repository

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsError, Json}
import reactivemongo.api.ReadPreference
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.customs.managesubscription.repository.{RecipientDetailsCacheRepository, RecipientDetailsRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class RecipientDetailsRepositorySpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val mockRecipientDetailsCacheRepo = mock[RecipientDetailsCacheRepository]
  private implicit val hc = HeaderCarrier()

  private val recipientDetailsCache = new RecipientDetailsRepository(mockRecipientDetailsCacheRepo)

  private val idAsString = "some-id"
  private val id = Id(idAsString)

  private val invalidCacheDataJson =
    Json.parse(
      """
        |{
        |  "recipientDetailsWithEori": {
        |    "invalid": "invalid"
        |  }
        |}""".stripMargin)

  "recipient details cache" should {

    "return JsError when invalid Json for recipientDetailsWithEori found" in {
      val invalidCache = Cache(mock[Id], Some(invalidCacheDataJson))
      when(mockRecipientDetailsCacheRepo.findById(meq(id), any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(invalidCache)))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.left.get shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }

    "return JsError when no data found" in {
      val invalidCache = Cache(mock[Id], None)
      when(mockRecipientDetailsCacheRepo.findById(meq(id), any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(invalidCache)))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.left.get shouldBe JsError("No data is saved for the formBundleId: some-id")
    }

    "return error when data not found for recipientDetailsWithEori" in {
      val previousJson = {
        Json.parse(
          """
            |{
            |  "recipientDetails": {
            |    "invalid": "invalid"
            |  }
            |}""".stripMargin)
      }
      val invalidCache = Cache(mock[Id], Some(previousJson))
      when(mockRecipientDetailsCacheRepo.findById(meq(id), any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(Some(invalidCache)))

      val result = await(recipientDetailsCache.recipientDetailsForBundleId(idAsString))

      result.left.get shouldBe JsError("Data saved in db is invalid for formBundleId: some-id")
    }
  }

  override protected def beforeEach(): Unit = {
    reset(mockRecipientDetailsCacheRepo)
  }
}
