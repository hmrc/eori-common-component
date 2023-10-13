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

package unit.services

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.customs.managesubscription.connectors.{GetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.customs.managesubscription.domain.vat._
import uk.gov.hmrc.customs.managesubscription.services.GetVatCustomerInformationService
import util.BaseSpec

import scala.concurrent.Future

class GetVatCustomerInformationServiceSpec extends BaseSpec {
  private val mockConnector = mock[GetVatCustomerInformationConnector]
  private val service       = new GetVatCustomerInformationService(mockConnector)
  private val vrn           = "123456789"

  "getVatCustomerInformation" should {

    "Return right value VatCustomerInformation when connector is success" in {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val vatCustomerInformation: Either[ResponseError, VatCustomerInformation] = Right(
        VatCustomerInformation(
          VatApprovedInformation(
            VatCustomerDetails(Some(format.parse("2001-01-01"))),
            VatCustomerInformationPPOB(Some(VatCustomerAddress(Some("SW1A 2BQ"))))
          )
        )
      )

      mockGetVatCustomerInformationConnector(EitherT[Future, ResponseError, VatCustomerInformation] {
        Future.successful(vatCustomerInformation)
      })

      val result = service.getVatCustomerInformation(vrn)
      result.value.toString shouldBe Future.successful(vatCustomerInformation).toString
    }

    "Return left value ResponseError when connector is not OK" in {
      val responseError: Either[ResponseError, VatCustomerInformation] =
        Left(ResponseError(NOT_FOUND, "an error message"))

      mockGetVatCustomerInformationConnector(EitherT[Future, ResponseError, VatCustomerInformation] {
        Future.successful(responseError)
      })

      val result = service.getVatCustomerInformation(vrn)
      result.value.toString shouldBe Future.successful(responseError).toString
    }
  }

  def mockGetVatCustomerInformationConnector(response: EitherT[Future, ResponseError, VatCustomerInformation]): Unit =
    when(mockConnector.getVatCustomerInformation(any())) thenReturn response

}
