/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.customs.managesubscription.connectors.{CustomsDataStoreConnector, MicroserviceAuthConnector}
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class CustomsUpdateDataStoreControllerSpec
    extends PlaySpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with GuiceOneAppPerSuite {

  val dataStoreRequest      = DataStoreRequest("ZZ123456789000", "a@example.com", "2018-07-05T09:08:12Z")
  val data                  = Json.toJson(dataStoreRequest)
  val validDataStoreRequest = FakeRequest("POST", "/customs/update/datastore").withJsonBody(data)

  private val mockDataStoreConnector   = mock[CustomsDataStoreConnector]
  private val mockAuthConnector        = mock[MicroserviceAuthConnector]
  private val mockControllerComponents = mock[ControllerComponents]

  override def fakeApplication() = new GuiceApplicationBuilder()
    .overrides(bind[MicroserviceAuthConnector].toInstance(mockAuthConnector))
    .overrides(bind[CustomsDataStoreConnector].toInstance(mockDataStoreConnector))
    .build()

  override protected def beforeEach(): Unit = {
    reset(mockControllerComponents, mockAuthConnector, mockDataStoreConnector)
    when(
      mockAuthConnector.authorise(meq(AuthProviders(GovernmentGateway)), meq(EmptyRetrieval))(
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    )
      .thenReturn(Future.successful(()))
  }

  "CustomsUpdateDataStoreController POST" should {

    "respond with status 204 for a valid request" in {

      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(204, "")))
      val result = await(route(app, validDataStoreRequest).get)
      result.header.status mustBe Status.NO_CONTENT
    }

    "respond with status 400 for a invalid request" in {
      when(mockDataStoreConnector.updateDataStore(any[DataStoreRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(204, "")))
      val invalidRequest = FakeRequest("POST", "/customs/update/datastore").withJsonBody(Json.toJson(""))
      val result         = await(route(app, invalidRequest).get)
      result.header.status mustBe Status.BAD_REQUEST
    }

    "respond with status 404 for a invalid request url" in {
      val invalidRequest = FakeRequest("POST", "/customs/update/datastore-not-found").withJsonBody(Json.toJson(""))
      val result         = await(route(app, invalidRequest).get)
      result.header.status mustBe Status.NOT_FOUND
    }
  }

}
