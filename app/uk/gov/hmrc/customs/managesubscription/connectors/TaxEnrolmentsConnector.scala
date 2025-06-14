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

package uk.gov.hmrc.customs.managesubscription.connectors

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.customs.managesubscription.BuildUrl
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.domain.protocol.TaxEnrolmentsRequest
import uk.gov.hmrc.customs.managesubscription.models.events.{SubscriberCall, SubscriberRequest, SubscriberResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsConnector @Inject() (buildUrl: BuildUrl, httpClient: HttpClientV2, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  private val baseUrl = buildUrl("tax-enrolments")

  def enrol(request: TaxEnrolmentsRequest, formBundleId: String)(implicit hc: HeaderCarrier): Future[Int] = {
    val url = s"$baseUrl/$formBundleId/subscriber"

    // $COVERAGE-OFF$Loggers
    logger.info(s"putUrl: $url")
    logger.debug(s"Tax enrolment: $url, body: $request and headers: $hc")
    // $COVERAGE-ON

    httpClient
      .put(new URI(url).toURL)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map {
        response =>
          logResponse(response)
          auditCall(url, request, response)
          response.status
      }
  }

  private def auditCall(url: String, request: TaxEnrolmentsRequest, response: HttpResponse)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val subscriberRequest  = SubscriberRequest(request)
    val subscriberResponse = SubscriberResponse(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-subscriber-call",
      path = url,
      details = Json.toJson(SubscriberCall(subscriberRequest, subscriberResponse)),
      eventType = "SubscriberCall"
    )
  }

  private def logResponse(response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug(s"Tax enrolment complete. Status:${response.status}")
    else
      logger.warn(s"Tax enrolment request failed with response $response")

}
