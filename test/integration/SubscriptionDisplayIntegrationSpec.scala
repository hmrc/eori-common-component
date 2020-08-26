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

package integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.OK
import uk.gov.hmrc.customs.managesubscription.connectors.SubscriptionDisplayConnector
import uk.gov.hmrc.http.HeaderCarrier
import util.RequestHeaders.{ACCEPT_HEADER, AUTHORISATION_HEADER, CONTENT_TYPE_HEADER}
import util.SubscriptionDisplayService

class SubscriptionDisplayIntegrationSpec extends IntegrationTestsWithDbSpec with SubscriptionDisplayService with ScalaFutures {

  val validHeaders: Seq[(String, String)] = Seq(AUTHORISATION_HEADER, CONTENT_TYPE_HEADER, ACCEPT_HEADER)
  private implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(validHeaders: _*)
  private val expectedUrl = "/subscriptions/subscriptiondisplay/v1"
  private lazy val queryParams = ("taxPayerID" -> "SAFEID") :: List("regime" -> "CDS", "acknowledgementReference" -> "UUID")
  private lazy val expectedUrlWithQueryString = expectedUrl + "?taxPayerID=SAFEID&regime=CDS&acknowledgementReference=UUID"
  private lazy val subscriptionDisplayConnector = app.injector.instanceOf[SubscriptionDisplayConnector]

  override def beforeAll: Unit = {
    startMockServer()
  }

  override def afterAll: Unit = {
    stopMockServer()
  }

  "SubscriptionDisplayConnector" should {
    "call subscription display api with correct url and query string" in {
      scala.concurrent.Await.ready(subscriptionDisplayConnector.callSubscriptionDisplay(queryParams), defaultTimeout)
      WireMock.verify(getRequestedFor(urlEqualTo(expectedUrlWithQueryString)))
    }

    "return successful future with eori number when subscription display api returns OK response" in {
      returnSubscriptionDisplayResponse(expectedUrlWithQueryString, OK)
      subscriptionDisplayConnector.callSubscriptionDisplay(queryParams).futureValue shouldBe Some("123456789")
    }
  }
}
