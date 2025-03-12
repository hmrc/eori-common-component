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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.MimeTypes
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentType, status, stubControllerComponents}
import uk.gov.hmrc.customs.managesubscription.connectors.ResponseError
import uk.gov.hmrc.customs.managesubscription.controllers.GetVatCustomerInformationController
import uk.gov.hmrc.customs.managesubscription.domain.vat._
import uk.gov.hmrc.customs.managesubscription.services.GetVatCustomerInformationService
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._
import util.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class GetVatCustomerInformationControllerSpec extends UnitSpec with BeforeAndAfterEach {

  private val mockService               = mock[GetVatCustomerInformationService]
  private val mockStubBehaviour         = mock[StubBehaviour]
  implicit val cc: ControllerComponents = stubControllerComponents()
  implicit val ec: ExecutionContext     = scala.concurrent.ExecutionContext.global

  private val fakeRequest =
    FakeRequest(method = "GET", path = "/someRootPath?vrn=12345678").withHeaders("Authorization" -> "Token some-token")

  private val controller =
    new GetVatCustomerInformationController(mockService, BackendAuthComponentsStub(mockStubBehaviour), cc)

  private val vrn = "123456789"

  private val expectedPredicate =
    Predicate.Permission(Resource(ResourceType("eori-common-component"), ResourceLocation("vat")), IAAction("WRITE"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.unit)
  }

  override protected def afterEach(): Unit = {
    reset(mockService)
    reset(mockStubBehaviour)
    super.afterEach()
  }

  "getVatCustomerInformation" should {

    "return OK for getVatCustomerInformation service call with VatCustomerInformation" in {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val vatCustomerInformation: Either[ResponseError, VatCustomerInformation] = Right(
        VatCustomerInformation(
          VatApprovedInformation(
            VatCustomerDetails(Some(format.parse("2001-01-01"))),
            VatCustomerInformationPPOB(Some(VatCustomerAddress(Some("SW1A 2BQ"))))
          )
        )
      )

      mockGetVatCustomerInformationService(EitherT[Future, ResponseError, VatCustomerInformation] {
        Future.successful(vatCustomerInformation)
      })

      val result = controller.getVatCustomerInformation(vrn)(fakeRequest)
      status(result) shouldBe OK
      contentType(result) shouldBe Some(MimeTypes.JSON)
      contentAsJson(result) shouldBe Json.parse(
        """ {"effectiveRegistrationDate":978307200000,"postCode":"SW1A 2BQ"} """
      )
    }
  }

  "return NOT_FOUND for getVatCustomerInformation service call with ResponseError as NOT_FOUND" in {
    val responseError: Either[ResponseError, VatCustomerInformation] =
      Left(ResponseError(NOT_FOUND, "an error message"))

    mockGetVatCustomerInformationService(EitherT[Future, ResponseError, VatCustomerInformation] {
      Future.successful(responseError)
    })

    val result = controller.getVatCustomerInformation(vrn)(fakeRequest)
    status(result) shouldBe NOT_FOUND
    contentType(result) shouldBe Some(MimeTypes.JSON)
    contentAsJson(result) shouldBe Json.parse(""" {"status":404,"error":"an error message"} """)
  }

  def mockGetVatCustomerInformationService(response: EitherT[Future, ResponseError, VatCustomerInformation]): Unit =
    when(mockService.getVatCustomerInformation(any())) thenReturn response

}
