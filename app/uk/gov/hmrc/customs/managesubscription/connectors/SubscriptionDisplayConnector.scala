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

package uk.gov.hmrc.customs.managesubscription.connectors

import play.api.Logger
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDisplayConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def callSubscriptionDisplay(
    queryParams: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val url     = appConfig.subscriptionDisplayUrl + makeQueryString(queryParams)
    val headers = generateHeadersWithBearerToken
    auditRequestHeaders(headers, url)
    httpClient.GET(url, Seq(), headers) map { response =>
      auditResponse(response, url)
      logResponse(response.status)
      extractEoriNumber(Json.parse(response.body))
    }
  }

  private def extractEoriNumber: JsValue => Option[String] = json =>
    (json \ "subscriptionDisplayResponse" \ "responseDetail" \ "EORINo").asOpt[String]

  private def logResponse: Int => Unit = {
    case OK     => logger.info("Subscription display request is successful")
    case status => logger.warn(s"Subscription display request is failed with status $status")
  }

  private def makeQueryString(queryParams: Seq[(String, String)]): String = {
    val params: String = queryParams map Function.tupled((k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}") mkString "&"
    if (params.isEmpty) "" else s"?$params"
  }

  private def generateHeadersWithBearerToken: Seq[(String, String)] = {
    val clock = Clock.systemDefaultZone()
    Seq(
      DATE               -> DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock.withZone(ZoneId.of("GMT")))),
      "X-Correlation-ID" -> UUID.randomUUID().toString,
      X_FORWARDED_HOST   -> "MDTP",
      ACCEPT             -> MimeTypes.JSON,
      AUTHORIZATION      -> s"Bearer ${appConfig.subscriptionDisplayBearerToken}"
    )
  }

  private def auditRequestHeaders(headers: Seq[(String, String)], url: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "SubscriptionDisplayRequestSubmitted",
      path = url,
      detail = Map("headers" -> s"$headers"),
      auditType = "SubscriptionDisplayRequest"
    )

  private def auditResponse(response: HttpResponse, url: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "SubscriptionDisplayResponseReceived",
      path = url,
      detail = Map("status" -> s"${response.status}", "message" -> s"${response.body}"),
      auditType = "SubscriptionDisplayResponse"
    )

}
