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

package repository

import base.IntegrationTestsWithDbSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, JsValue, Json}
import uk.gov.hmrc.customs.managesubscription.domain.protocol.{Email, Eori}
import uk.gov.hmrc.customs.managesubscription.repository.Save4LaterRepository
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, *}
import scala.language.postfixOps

class Save4LaterRepositoryIntegrationSpec
    extends IntegrationTestsWithDbSpec with MockitoSugar with MongoSupport with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(200 millis), interval = scaled(100 millis))

  val mockServiceConfig: ServicesConfig = mock[ServicesConfig]

  val email: Email = Email("john.doe@digital.hmrc.gov.uk")
  val eori: Eori = Eori("GB0123456789")
  val mockTimeStampSupport = new CurrentTimestampSupport()
  when(mockServiceConfig.getDuration(any[String])).thenReturn(Duration(5000, TimeUnit.SECONDS))
  val repository = new Save4LaterRepository(mockServiceConfig, mongoComponent, mockTimeStampSupport)
  val id         = "id-1"
  val key1       = "key-1"
  val data: JsValue = Json.toJson(eori)

  "recipient details repository" should {
    "save  the details" in {
      repository.save(id, key1, data).futureValue shouldBe ((): Unit)
    }

    "update the details" in {
      val data = Json.toJson(eori)
      repository.save(id, key1, data).futureValue shouldBe ((): Unit)
    }

    "fetch Details correctly" in {
      val data = Json.toJson(eori)
      repository.save(id, key1, data).futureValue
      repository.findByIdAndKey(id, key1).futureValue shouldBe Some(data)
    }

    "remove key from collection" in {
      val eori1  = Json.toJson(eori)
      val email1 = Json.toJson(email)
      repository.save(id, "eori", eori1).futureValue
      repository.save(id, "email", email1).futureValue
      repository.findByIdAndKey(id, "email").futureValue shouldBe Some(email1)
      repository.removeKeyById(id, "eori").futureValue shouldBe true
      repository.findByIdAndKey(id, "email").futureValue shouldBe Some(email1)
      repository.findByIdAndKey(id, "eori").futureValue shouldBe None

    }
    "remove non existing key from collection" in {
      val email1 = Json.toJson(email)
      repository.save(id, "email", email1).futureValue
      repository.removeKeyById(id, "eori").futureValue shouldBe false
    }
    "remove non existing id from collection" in {
      repository.save(id, "email", JsNull).futureValue shouldBe ((): Unit)
      repository.removeKeyById("id-not-exist", "eori").futureValue shouldBe false
    }

    "remove id from collection with remove failed " in {
      val spyRepository = Mockito.spy(repository)
      val eori1         = Json.toJson(eori)
      val email1        = Json.toJson(email)
      spyRepository.save(id, "eori", eori1).futureValue
      spyRepository.save(id, "email", email1).futureValue
      when(spyRepository.deleteEntity(id)).thenReturn(Future.failed(new RuntimeException("future Failed")))
      spyRepository.remove(id).futureValue shouldBe false
    }

    "remove id from collection with removeKeyById failed " in {
      val spyRepository = Mockito.spy(repository)
      val eori1         = Json.toJson(eori)
      val email1        = Json.toJson(email)
      spyRepository.save(id, "eori", eori1).futureValue
      spyRepository.save(id, "email", email1).futureValue
      when(spyRepository.delete(id)(DataKey("eori"))).thenReturn(Future.failed(new RuntimeException("future Failed")))
      spyRepository.removeKeyById(id, "eori").futureValue shouldBe false
    }

    "remove id from collection" in {
      val eori1  = Json.toJson(eori)
      val email1 = Json.toJson(email)
      repository.save(id, "eori", eori1).futureValue
      repository.save(id, "email", email1).futureValue
      repository.remove(id).futureValue shouldBe true
      repository.findByIdAndKey(id, "email").futureValue shouldBe None
      repository.findByIdAndKey(id, "eori").futureValue shouldBe None

    }

  }
}
