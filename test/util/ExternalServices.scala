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

package util

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.NO_CONTENT
import util.TestData.formBundleId

trait MockHelpers {
  val stub: MappingBuilder => StubMapping = mb => stubFor(mb)

  val ReturnsSuccessfulResponse: ResponseDefinitionBuilder = aResponse().withStatus(OK)

  val ReturnsUnauthorizedResponse: ResponseDefinitionBuilder = aResponse().withStatus(UNAUTHORIZED)

  val ReturnsNoContent: ResponseDefinitionBuilder = aResponse().withStatus(NO_CONTENT)
}

trait MDTPEmailService extends WireMockRunner {

  val MDTPEmailServicePath: UrlPattern = urlMatching("/hmrc/email")

  def mdtpEmailServiceIsRunning(): Unit =
    stubFor(
      post(MDTPEmailServicePath).willReturn(
        aResponse()
          .withStatus(NO_CONTENT)
      )
    )

  def requestMadeToMDTPEmailService(): String =
    wireMockServer.findRequestsMatching(postRequestedFor(MDTPEmailServicePath).build())
      .getRequests.get(0).getBodyAsString

}

trait TaxEnrolmentService extends WireMockRunner {
  private def taxEnrolmentsUrl(formBundleId: String) = s"/tax-enrolments/subscriptions/$formBundleId/subscriber"

  def returnEnrolmentResponseWhenReceiveRequest(url: String, request: String, status: Int): Unit =
    stubFor(
      put(urlEqualTo(url))
        .withRequestBody(equalToJson(request))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def verifyTaxEnrolmentsCalled(formBundleId: String = formBundleId): Unit =
    verify(putRequestedFor(urlEqualTo(taxEnrolmentsUrl(formBundleId))))

}

trait CustomsDataStoreService extends WireMockRunner {

  def returnCustomsDataStoreResponse(url: String, request: String, status: Int): Unit =
    stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(request))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}

trait SubscriptionDisplayService extends WireMockRunner {
  private val responseBody = """{"subscriptionDisplayResponse": {"responseDetail": {"EORINo": "ZZ123456789000"}}}"""

  def returnSubscriptionDisplayResponse(url: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
            .withBody(responseBody)
        )
    )

}

object ExternalServicesConfig {
  val Port: Int    = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11111").toInt
  val Host: String = "localhost"
}
