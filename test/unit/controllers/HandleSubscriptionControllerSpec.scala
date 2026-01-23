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

package unit.controllers

import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.*
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, status, stubControllerComponents, stubPlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.managesubscription.controllers.{DigitalHeaderValidator, HandleSubscriptionController}
import uk.gov.hmrc.customs.managesubscription.services.TaxEnrolmentsService
import uk.gov.hmrc.http.HeaderCarrier
import util.RequestHeaders.*
import util.TestData.HandleSubscription.*
import util.TestData.*
import util.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class HandleSubscriptionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val cc: ControllerComponents = stubControllerComponents()

  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]
  private val mockAuthConnector        = mock[AuthConnector]

  private val mockDigitalHeaderValidator = new DigitalHeaderValidator(stubPlayBodyParsers(NoMaterializer))(
    ExecutionContext.global
  )

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val controller =
    new HandleSubscriptionController(mockTaxEnrolmentsService, cc, mockDigitalHeaderValidator, mockAuthConnector)

  override def beforeEach(): Unit = {
    reset(mockTaxEnrolmentsService)
    reset(mockAuthConnector)
    when(mockAuthConnector.authorise[Unit](any, any)(any, any)).thenReturn(Future.successful(()))
  }

  "HandleSubscriptionController" should {
    "respond with status 204 (No Content) if the request is valid and status SUCCEEDED" in {
      when(
        mockTaxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(
          meq(formBundleId),
          meq(recipientDetails),
          meq(taxPayerId),
          meq(Option(eori)),
          meq(emailVerificationTimestamp),
          meq(safeId)
        )(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(200))
      testSubmitResult(validRequest) { result =>
        status(result) shouldBe NO_CONTENT

        verify(mockTaxEnrolmentsService).saveRecipientDetailsAndCallTaxEnrolment(
          meq(formBundleId),
          meq(recipientDetails),
          meq(taxPayerId),
          meq(Option(eori)),
          meq(emailVerificationTimestamp),
          meq(safeId)
        )(any[HeaderCarrier], any[ExecutionContext])
      }
    }

    "respond with status 500 (Internal Server Error) if the request fails" in {
      when(
        mockTaxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(
          meq(formBundleId),
          meq(recipientDetails),
          meq(taxPayerId),
          meq(Option(eori)),
          meq(emailVerificationTimestamp),
          meq(safeId)
        )(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(404))
      testSubmitResult(validRequest) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    //    can not simulate/ should not simulare especialy RuntimeExceptions
    "respond with status 500 if the request processing fails" in {
      when(
        mockTaxEnrolmentsService.saveRecipientDetailsAndCallTaxEnrolment(
          meq(formBundleId),
          meq(recipientDetails),
          meq(taxPayerId),
          meq(Option(eori)),
          meq(emailVerificationTimestamp),
          meq(safeId)
        )(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(emulatedServiceFailure))
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

  private def testSubmitResult(request: Request[AnyContent])(test: Future[Result] => Unit): Unit =
    test(controller.handle().apply(request))

}
