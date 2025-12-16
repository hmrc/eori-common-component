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

import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.customs.managesubscription.connectors.{CustomsDataStoreConnector, MicroserviceAuthConnector}
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.{ExecutionContext, Future}

class CustomsUpdateDataStoreControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with GuiceOneAppPerSuite {

  val dataStoreRequest: DataStoreRequest = DataStoreRequest("ZZ123456789000", "a@example.com", "2018-07-05T09:08:12Z")
  val data: JsValue                      = Json.toJson(dataStoreRequest)

  val validDataStoreRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest("POST", "/customs/update/datastore").withJsonBody(data).withHeaders(
      "Authorization" -> "Token some-token"
    )

  private val mockDataStoreConnector   = mock[CustomsDataStoreConnector]
  private val mockAuthConnector        = mock[MicroserviceAuthConnector]
  private val mockControllerComponents = mock[ControllerComponents]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val cc: ControllerComponents              = stubControllerComponents()
  private val mockStubBehaviour                      = mock[StubBehaviour]

  private val expectedPredicate =
    Predicate.Permission(Resource(ResourceType("eori-common-component"), ResourceLocation("cds")), IAAction("WRITE"))

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(
      bind[MicroserviceAuthConnector].toInstance(mockAuthConnector),
      bind[CustomsDataStoreConnector].toInstance(mockDataStoreConnector),
      bind[BackendAuthComponents].toInstance(BackendAuthComponentsStub(mockStubBehaviour))
    )
    .build()

  override protected def beforeEach(): Unit = {
    reset(mockControllerComponents, mockAuthConnector, mockDataStoreConnector, mockStubBehaviour)
    when(
      mockAuthConnector.authorise(meq(AuthProviders(GovernmentGateway)), meq(EmptyRetrieval))(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    )
      .thenReturn(Future.successful(()))

    when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
  }

  "CustomsUpdateDataStoreController POST" should {

    "respond with status 204 for a valid request" in {
      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(204, "")))
      val result = await(route(app, validDataStoreRequest).get)
      result.header.status mustBe Status.NO_CONTENT
    }

    "respond with status 500 when 400 is returned from the connector" in {
      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(400, "")))
      val result = await(route(app, validDataStoreRequest).get)
      result.header.status mustBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with status 500 when 500 is returned from the connector" in {
      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(500, "")))
      val result = await(route(app, validDataStoreRequest).get)
      result.header.status mustBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with status 400 for a invalid request" in {
      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(204, "")))
      val invalidRequest = FakeRequest("POST", "/customs/update/datastore").withJsonBody(Json.toJson("")).withHeaders(
        "Authorization" -> "Token some-token"
      )
      val result = await(route(app, invalidRequest).get)
      result.header.status mustBe Status.BAD_REQUEST
    }

    "respond with status 404 for a invalid request url" in {
      val invalidRequest = FakeRequest("POST", "/customs/update/datastore-not-found").withJsonBody(
        Json.toJson("")
      ).withHeaders("Authorization" -> "Token some-token")
      val result = await(route(app, invalidRequest).get)
      result.header.status mustBe Status.NOT_FOUND
    }
  }

}
