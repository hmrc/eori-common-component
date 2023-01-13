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

import akka.stream.testkit.NoMaterializer
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.managesubscription.controllers.{MessagingHeaderValidator, SubscriptionResultController}
import uk.gov.hmrc.customs.managesubscription.services.SubscriptionCompleteBusinessService
import uk.gov.hmrc.http.HeaderCarrier
import util.UnitSpec
import util.RequestHeaders
import util.TestData.SubscriptionResult._
import util.TestData._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubscriptionResultControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockBusinessService          = mock[SubscriptionCompleteBusinessService]
  private val mockControllerComponents     = mock[ControllerComponents]
  private val mockMessagingHeaderValidator = new MessagingHeaderValidator(stubPlayBodyParsers(NoMaterializer))(global)

  private val controller =
    new SubscriptionResultController(mockBusinessService, mockControllerComponents, mockMessagingHeaderValidator)

  override def beforeEach(): Unit =
    reset(mockBusinessService)

  "SubscriptionCompleteController" should {
    "respond with status 204 if the request is valid and status SUCCEEDED" in {
      when(
        mockBusinessService.onSubscriptionStatus(meq(successSubscriptionComplete), ArgumentMatchers.eq(formBundleId))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(()))

      testSubmitResult(validSucceededRequest) { result =>
        status(result) shouldBe NO_CONTENT
        verify(mockBusinessService)
          .onSubscriptionStatus(meq(successSubscriptionComplete), meq(formBundleId))(any[HeaderCarrier])
      }
    }

    "respond with status 204 if the request is valid and status FAILED" in {
      when(
        mockBusinessService.onSubscriptionStatus(meq(failedSubscriptionComplete), meq(formBundleId))(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(()))

      testSubmitResult(validErrorRequest) { result =>
        status(result) shouldBe NO_CONTENT

        verify(mockBusinessService)
          .onSubscriptionStatus(meq(failedSubscriptionComplete), meq(formBundleId))(any[HeaderCarrier])
      }
    }

    "respond with status 500 if the request processing fails" in {
      when(
        mockBusinessService.onSubscriptionStatus(meq(successSubscriptionComplete), meq(formBundleId))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.failed(emulatedServiceFailure))

      testSubmitResult(validSucceededRequest) { result =>
        val caught = intercept[RuntimeException](await(result))
        caught shouldBe emulatedServiceFailure
      }
    }

    "respond with 400 if json is empty" in {
      testSubmitResult(
        FakeRequest()
          .withHeaders(RequestHeaders.ACCEPT_HEADER, RequestHeaders.CONTENT_TYPE_HEADER)
      ) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

  }

  "state" should {
    passMandatoryCheck(validSucceededJsonBody, stateField)((m, v) => m.copy(state = v))

    "accept SUCCESS" in {
      when(
        mockBusinessService.onSubscriptionStatus(meq(successSubscriptionComplete), meq(formBundleId))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(()))
      testSubmitResult(mkRequest(validSucceededModel)) {
        result =>
          status(result) shouldBe NO_CONTENT
      }
    }

    "accept FAILED" in {
      when(
        mockBusinessService.onSubscriptionStatus(meq(failedSubscriptionComplete), meq(formBundleId))(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(()))
      testSubmitResult(mkRequest(validErrorModel)) {
        result =>
          status(result) shouldBe NO_CONTENT
      }
    }

    "reject unknown values" in {
      testSubmitResult(mkRequest(validSucceededModel.copy(state = Some("unknown")))) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
  }

  private def passMandatoryCheck(from: JsValue, fieldName: String)(
    modelFieldModifier: (RequestModel, Option[String]) => RequestModel
  ) = {
    "be mandatory" in {
      testSubmitResult(mkRequest(pruneField(from, fieldName))) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "not be empty" in {
      testSubmitResult(mkRequest(modelFieldModifier(validSucceededModel, Some("")))) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
  }

  private def testSubmitResult(request: Request[AnyContent])(test: Future[Result] => Unit) {
    test(controller.updateStatus(formBundleId).apply(request))
  }

}
