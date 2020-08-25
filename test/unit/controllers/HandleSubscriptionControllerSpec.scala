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

package unit.controllers

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.mvc._
import play.api.test.{FakeRequest, NoMaterializer}
import play.api.test.Helpers.stubPlayBodyParsers
import uk.gov.hmrc.customs.managesubscription.controllers.{DigitalHeaderValidator, HandleSubscriptionController}
import uk.gov.hmrc.customs.managesubscription.services.TaxEnrolmentsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders._
import util.TestData.HandleSubscription._
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}

class HandleSubscriptionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]
  private val mockControllerComponents = mock[ControllerComponents]
  private val mockDigitalHeaderValidator = new DigitalHeaderValidator(stubPlayBodyParsers(NoMaterializer))(ExecutionContext.global)

  private val controller = new HandleSubscriptionController(mockTaxEnrolmentsService, mockControllerComponents, mockDigitalHeaderValidator)

  override def beforeEach(): Unit = {
    reset(mockTaxEnrolmentsService)
  }

  "HandleSubscriptionController" should {
    "respond with status 204 if the request is valid and status SUCCEEDED" in {
      when(mockTaxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(meq(formBundleId), meq(recipientDetails), meq(taxPayerId), meq(Option(eori)), meq(emailVerificationTimestamp), meq(safeId))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(200))
      testSubmitResult(validRequest) { result =>
        status(result) shouldBe NO_CONTENT

        verify(mockTaxEnrolmentsService).saveRecipientDetailsAndCallTaxEnrolment(meq(formBundleId), meq(recipientDetails), meq(taxPayerId), meq(Option(eori)), meq(emailVerificationTimestamp), meq(safeId))(any[HeaderCarrier], any[ExecutionContext])
      }
    }

    //    can not simulate/ should not simulare especialy RuntimeExceptions
    "respond with status 500 if the request processing fails" in {
      when(mockTaxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(meq(formBundleId), meq(recipientDetails), meq(taxPayerId), meq(Option(eori)), meq(emailVerificationTimestamp), meq(safeId))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(emulatedServiceFailure))
      testSubmitResult(validRequest) { result =>
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown shouldBe emulatedServiceFailure
      }
    }

    "respond with 400 if json is empty" in {
      testSubmitResult(FakeRequest().withHeaders(AUTHORISATION_HEADER, ACCEPT_HEADER, CONTENT_TYPE_HEADER)) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "respond with status 400 if json is invalid" in {
      testSubmitResult(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  private def testSubmitResult(request: Request[AnyContent])(test: Future[Result] => Unit) {
    test(controller.handle().apply(request))
  }
}
