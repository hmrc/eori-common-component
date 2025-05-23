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

package unit.controllers

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.customs.managesubscription.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.managesubscription.repository.Save4LaterRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import util.TestData._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class Save4LaterControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with GuiceOneAppPerSuite {

  val id            = "id-1"
  val key1          = "key-1"
  val data: JsValue = Json.toJson(recipientDetails)

  val validPutRequestWithCache: FakeRequest[AnyContentAsJson] =
    FakeRequest("PUT", "/save4later/id-1/key-1").withJsonBody(data).withHeaders("Authorization" -> "Token some-token")

  val validGetRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/save4later/id-1/key-1").withHeaders("Authorization" -> "Token some-token")

  val validDeleteRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("DELETE", "/save4later/id-1").withHeaders("Authorization" -> "Token some-token")

  val validDeleteKeyRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("DELETE", "/save4later/id-1/key-1").withHeaders("Authorization" -> "Token some-token")

  private val mockSave4LaterRepository = mock[Save4LaterRepository]
  private val mockAuthConnector        = mock[MicroserviceAuthConnector]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val cc: ControllerComponents              = stubControllerComponents()
  private val mockStubBehaviour                      = mock[StubBehaviour]

  private val expectedPredicate =
    Predicate.Permission(Resource(ResourceType("eori-common-component"), ResourceLocation("save")), IAAction("WRITE"))

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> ("mongodb://localhost:27017/cds" + UUID.randomUUID().toString))
    .overrides(
      bind[MicroserviceAuthConnector].toInstance(mockAuthConnector),
      bind[Save4LaterRepository].toInstance(mockSave4LaterRepository),
      bind[BackendAuthComponents].toInstance(BackendAuthComponentsStub(mockStubBehaviour))
    )
    .build()

  override protected def beforeEach(): Unit = {
    reset(mockAuthConnector, mockSave4LaterRepository, mockStubBehaviour)
    when(
      mockAuthConnector.authorise(meq(AuthProviders(GovernmentGateway)), meq(EmptyRetrieval))(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    )
      .thenReturn(Future.successful(()))

    when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)

  }

  "Save4LaterController PUT" should {

    "respond with status 201 for a valid request updated in mongo" in {
      when(mockSave4LaterRepository.save(meq(id), meq(key1), meq(data)))
        .thenReturn(Future.successful(()))
      val result = await(route(app, validPutRequestWithCache).get)
      result.header.status mustBe Status.CREATED
    }

    "respond with status 404 for a invalid request update in mongo" in {
      when(mockSave4LaterRepository.save(any(), any(), any()))
        .thenReturn(Future.successful(()))
      val invalidRequest = FakeRequest("PUT", "/save4later/id").withJsonBody(Json.toJson("")).withHeaders(
        "Authorization" -> "Token some-token"
      )
      val result = await(route(app, invalidRequest).get)
      result.header.status mustBe Status.NOT_FOUND
    }

    "respond with status 400 for a invalid request update with no body" in {
      when(mockSave4LaterRepository.save(any(), any(), any()))
        .thenReturn(Future.successful(()))
      val invalidRequestWithNoBody =
        FakeRequest("PUT", "/save4later/id/key").withHeaders("Authorization" -> "Token some-token")
      val result = await(route(app, invalidRequestWithNoBody).get)
      result.header.status mustBe Status.BAD_REQUEST
    }

    "Save4LaterController GET" should {
      "respond with status 200 for id and key found in mongo" in {
        when(mockSave4LaterRepository.findByIdAndKey(meq(id), meq(key1)))
          .thenReturn(Future.successful(Some(data)))
        val result = route(app, validGetRequest).get
        result.futureValue.header.status mustBe Status.OK
        contentAsString(result) mustBe data.toString()
      }

      "respond with status 404 for id and key not found in mongo" in {
        when(mockSave4LaterRepository.findByIdAndKey(any(), any()))
          .thenReturn(Future.successful(None))
        val result = route(app, validGetRequest).get
        result.futureValue.header.status mustBe Status.NOT_FOUND
        contentAsString(result) mustBe "key:key-1 | id:id-1"
      }
    }

    "Save4LaterController DELETE" should {
      "respond with status 204 NoContent" in {
        when(mockSave4LaterRepository.removeKeyById(meq(id), meq(key1)))
          .thenReturn(Future.successful(true))
        val result = route(app, validDeleteKeyRequest).get
        result.futureValue.header.status mustBe Status.NO_CONTENT
      }

      "respond with status 404 for id and key not found in mongo" in {
        when(mockSave4LaterRepository.removeKeyById(any(), any()))
          .thenReturn(Future.successful(false))
        val result = route(app, validDeleteKeyRequest).get
        result.futureValue.header.status mustBe Status.NOT_FOUND
        contentAsString(result) mustBe "key:key-1 | id:id-1"
      }

      "respond with status 204 NoContent delete id" in {
        when(mockSave4LaterRepository.remove(meq(id)))
          .thenReturn(Future.successful(true))
        val result = route(app, validDeleteRequest).get
        result.futureValue.header.status mustBe Status.NO_CONTENT
      }

      "respond with status 404 for id not found in mongo" in {
        when(mockSave4LaterRepository.remove(any()))
          .thenReturn(Future.successful(false))
        val result = route(app, validDeleteRequest).get
        result.futureValue.header.status mustBe Status.NOT_FOUND
        contentAsString(result) mustBe "id:id-1"
      }
    }
  }
}
