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
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo}
import org.apache.pekko.dispatch.ThreadPoolConfig.defaultTimeout
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.NO_CONTENT
import uk.gov.hmrc.customs.managesubscription.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.HeaderCarrier
import util.CustomsDataStoreService
import util.TestData.{emailVerificationTimestamp, eori, recipientDetails}

class CustomsDataStoreIntegrationSpec
    extends IntegrationTestsWithDbSpec with CustomsDataStoreService with ScalaFutures {

  private implicit val hc: HeaderCarrier     = HeaderCarrier()
  private val expectedUrl                    = "/customs-data-store/update-email"
  private lazy val customsDataStoreConnector = app.injector.instanceOf[CustomsDataStoreConnector]

  private val dataStoreRequest =
    DataStoreRequest(eori.value, recipientDetails.recipientEmailAddress, emailVerificationTimestamp)

  private val dataStoreRequestQuery = s"""{
                                        |  "eori" : "${eori.value}",
                                        |  "address" : "${recipientDetails.recipientEmailAddress}",
                                        |  "timestamp" : "$emailVerificationTimestamp"
                                        |}""".stripMargin

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "CustomsDataStoreConnector" should {
    "call customs data store service with correct url and payload" in {
      scala.concurrent.Await.ready(customsDataStoreConnector.updateDataStore(dataStoreRequest), defaultTimeout)

      WireMock.verify(postRequestedFor(urlEqualTo(expectedUrl)).withRequestBody(equalToJson(dataStoreRequestQuery)))
    }

    "return successful future with correct status when customs data store service returns good status(204)" in {
      returnCustomsDataStoreResponse(expectedUrl, dataStoreRequestQuery, NO_CONTENT)
      customsDataStoreConnector.updateDataStore(dataStoreRequest).futureValue.status shouldBe NO_CONTENT
    }
  }
}
