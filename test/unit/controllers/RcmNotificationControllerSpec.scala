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
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.managesubscription.controllers.{DigitalHeaderValidator, RcmNotificationController}
import uk.gov.hmrc.customs.managesubscription.domain.RcmNotificationRequest
import uk.gov.hmrc.customs.managesubscription.services.EmailService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.UnitSpec
import util.TestData.HandleSubscription.validHeaders
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.{ExecutionContext, Future}

class RcmNotificationControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val rcmNotifyRequest =
    RcmNotificationRequest("a@b.com", "fullname", "GBXXXXXXXXX000", "Some Service", "2018-07-05T09:08:12.831Z")

  val validRcmNotifyRequest: Request[RcmNotificationRequest] =
    FakeRequest("POST", "/notify/rcm").withHeaders(validHeaders.toSeq: _*).withBody(rcmNotifyRequest)


  implicit val cc = stubControllerComponents()

  private val mockEmailService = mock[EmailService]
  private val mockStubBehaviour = mock[StubBehaviour]
  private val expectedPredicate = Predicate.Permission(Resource(ResourceType("eori-common-component"), ResourceLocation("rcm-notification")), IAAction("WRITE"))

  private val mockDigitalHeaderValidator = new DigitalHeaderValidator(stubPlayBodyParsers(NoMaterializer))(
    ExecutionContext.global
  )

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val controller =
    new RcmNotificationController(mockEmailService, cc, mockDigitalHeaderValidator, BackendAuthComponentsStub(mockStubBehaviour))

  override protected def beforeEach(): Unit = {
    reset(mockEmailService)
    when(mockEmailService.sendRcmNotificationEmail(any[RcmNotificationRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(HttpResponse(status = 200, body = "")))
    reset(mockStubBehaviour)
    when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
  }

  "RcmNotificationController POST" should {

    "respond with status 204 for a valid request" in {
      testSubmitResult(
        FakeRequest("POST", "/notify/rcm").withHeaders(validHeaders.toSeq: _*).withBody(rcmNotifyRequest)
      ) { result =>
        status(result) shouldBe NO_CONTENT
      }
    }

  }

  private def testSubmitResult(request: Request[RcmNotificationRequest])(test: Future[Result] => Unit) = {
    test(controller.notifyRCM().apply(request))
  }

}
