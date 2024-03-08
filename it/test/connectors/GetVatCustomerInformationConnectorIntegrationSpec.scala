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

package connectors

import base.IntegrationTestsWithDbSpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, urlEqualTo}
import org.apache.pekko.dispatch.ThreadPoolConfig.defaultTimeout
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.Helpers.{OK, await}
import uk.gov.hmrc.customs.managesubscription.connectors.{GetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.customs.managesubscription.domain.vat._
import util.IntegrationFrameworkService

import scala.concurrent.Await

class GetVatCustomerInformationConnectorIntegrationSpec
    extends IntegrationTestsWithDbSpec with IntegrationFrameworkService {

  private lazy val connector = app.injector.instanceOf[GetVatCustomerInformationConnector]
  private val vrn            = "123456789"
  private val url            = s"/vat/customer/vrn/$vrn/information"

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  private val failedResponseBody =
    """{
                                     |  "failures": {
                                     |    "code": "INVALID_IDVALUE",
                                     |    "reason": "Submission has not passed validation. Invalid path parameter idValue."
                                     |  }
                                     |}""".stripMargin

  "GetVatCustomerInformationConnector" should {
    "call vatCustomerInformation with correct url successfully" in {
      Await.ready(connector.getVatCustomerInformation(vrn).value, defaultTimeout)
      WireMock.verify(getRequestedFor(urlEqualTo(url)))
    }

    "return successful VatCustomerInformation with OK status and response body" in {
      returnGetVatCustomerInformationResponse(
        url,
        OK,
        GetVatCustomerInformationConnectorSuccessResponse.successResponseBody
      )
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val expected = Right(
        VatCustomerInformation(
          VatApprovedInformation(
            VatCustomerDetails(Some(format.parse("2001-01-01"))),
            VatCustomerInformationPPOB(Some(VatCustomerAddress(Some("SW1A 2BQ"))))
          )
        )
      )
      val result: Either[ResponseError, VatCustomerInformation] = await(connector.getVatCustomerInformation(vrn).value)

      result shouldBe expected
    }

    "return failed VatCustomerInformation INTERNAL_SERVER_ERROR status and ResponseError for failed js conversion" in {
      returnGetVatCustomerInformationResponse(url, OK, failedResponseBody)
      val expected = Left(
        ResponseError(
          INTERNAL_SERVER_ERROR,
          """Invalid JSON returned: List((/approvedInformation,List(JsonValidationError(List(error.path.missing),List()))))"""
        )
      )
      val result: Either[ResponseError, VatCustomerInformation] = await(connector.getVatCustomerInformation(vrn).value)

      result shouldBe expected
    }

    "return failed VatCustomerInformation call with 400 status and ResponseError" in {
      returnGetVatCustomerInformationResponse(url, BAD_REQUEST, "some other failure")
      val result = await(connector.getVatCustomerInformation(vrn).value)
      result shouldBe Left(
        ResponseError(400, "Unexpected status from getVatCustomerInformation: 400 body: some other failure")
      )
    }
  }
}
