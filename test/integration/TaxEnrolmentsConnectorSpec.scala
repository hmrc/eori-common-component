/*
 * Copyright 2021 HM Revenue & Customs
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

package integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, putRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import uk.gov.hmrc.customs.managesubscription.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.customs.managesubscription.domain.protocol.TaxEnrolmentsRequest
import uk.gov.hmrc.http.HeaderCarrier
import util.TaxEnrolmentService
import util.TestData.TaxEnrolment.validRequestJson

class TaxEnrolmentsConnectorSpec extends IntegrationTestsWithDbSpec with TaxEnrolmentService with ScalaFutures {

  private implicit val hc                 = HeaderCarrier()
  private val formBundleId                = "bundle-id"
  private val expectedPutUrl              = s"/tax-enrolments/subscriptions/$formBundleId/subscriber"
  private lazy val taxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  private val taxEnrolmentsRequest = validRequestJson.as[TaxEnrolmentsRequest]

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "TaxEnrolmentConnector" should {
    "call tax enrolment service with correct url and payload" in {
      scala.concurrent.Await.ready(taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId), defaultTimeout)
      WireMock.verify(
        putRequestedFor(urlEqualTo(expectedPutUrl)).withRequestBody(equalToJson(validRequestJson.toString))
      )
    }

    "return successful future with correct status when enrolment status service returns good status(204)" in {
      returnEnrolmentResponseWhenReceiveRequest(expectedPutUrl, validRequestJson.toString, NO_CONTENT)
      taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId).futureValue shouldBe NO_CONTENT
    }

    "return successful future with correct status when enrolment status service returns any fail status" in {
      returnEnrolmentResponseWhenReceiveRequest(expectedPutUrl, validRequestJson.toString, BAD_REQUEST)
      taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId).futureValue shouldBe BAD_REQUEST
    }
  }
}
