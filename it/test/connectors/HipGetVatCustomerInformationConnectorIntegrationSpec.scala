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

package connectors

import base.IntegrationTestsWithDbSpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, urlEqualTo}
import org.apache.pekko.dispatch.ThreadPoolConfig.defaultTimeout
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, UNPROCESSABLE_ENTITY}
import play.api.test.Helpers.{OK, await}
import uk.gov.hmrc.customs.managesubscription.connectors.{HipGetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.customs.managesubscription.domain.vat.*
import util.IntegrationFrameworkService

import scala.concurrent.Await

class HipGetVatCustomerInformationConnectorIntegrationSpec
    extends IntegrationTestsWithDbSpec with IntegrationFrameworkService {

  private lazy val connector = app.injector.instanceOf[HipGetVatCustomerInformationConnector]
  private val vrn            = "123456789"
  private val url            = s"/etmp/RestAdapter/vat/subscription/vrn/$vrn"

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  private val failedResponseBody =
    """{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "Type of Failure",
      |        "reason": "Reason for Failure"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  private val notFoundBody =
    """
      |{
      |  "error": {
      |    "code": "002",
      |    "processingDate": "2026-03-09T12:34:46Z",
      |    "text": "ID not found"
      |  }
      |}
      |""".stripMargin

  "GetVatCustomerInformationConnector" should {
    "call vatCustomerInformation with correct url successfully" in {
      Await.ready(connector.getVatCustomerInformation(vrn).value, defaultTimeout)
      WireMock.verify(getRequestedFor(urlEqualTo(url)))
    }

    "return successful VatCustomerInformation with OK status and response body" in {
      returnGetVatCustomerInformationResponse(
        url,
        OK,
        GetVatCustomerInformationConnectorSuccessResponse.hipSuccessResponse
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
          """Invalid JSON returned: List((/success,List(JsonValidationError(List(error.path.missing),ArraySeq()))))"""
        )
      )
      val result: Either[ResponseError, VatCustomerInformation] = await(connector.getVatCustomerInformation(vrn).value)

      result shouldBe expected
    }

    "return failed NOT_FOUND status and ResponseError from converting an UNPROCESSABLE_ENTITY, 002, ID not found" in {
      returnGetVatCustomerInformationResponse(url, UNPROCESSABLE_ENTITY, notFoundBody)
      val expected = Left(
        ResponseError(
          NOT_FOUND,
          """Unexpected status from getVatCustomerInformation: 422 body:
            |{
            |  "error": {
            |    "code": "002",
            |    "processingDate": "2026-03-09T12:34:46Z",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin
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
