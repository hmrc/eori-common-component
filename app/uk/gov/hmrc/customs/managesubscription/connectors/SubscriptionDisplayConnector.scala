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

package uk.gov.hmrc.customs.managesubscription.connectors

import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.{URI, URLEncoder}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDisplayConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  audit: Auditable,
  headerProvider: DesHeaderProvider
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def callSubscriptionDisplay(queryParams: Seq[(String, String)])(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] = {
    val url     = appConfig.subscriptionDisplayUrl + makeQueryString(queryParams)
    val headers = headerProvider.generateHeadersWithBearerToken(appConfig.subscriptionDisplayBearerToken)

    auditRequestHeaders(headers, url)

    httpClient
      .get(new URI(url).toURL)
      .setHeader(headers: _*)
      .execute[HttpResponse]
      .map { response =>
        auditResponse(response, url)
        logResponse(response.status)
        extractEoriNumber(Json.parse(response.body))
      }
  }

  private def extractEoriNumber: JsValue => Option[String] = json =>
    (json \ "subscriptionDisplayResponse" \ "responseDetail" \ "EORINo").asOpt[String]

  // $COVERAGE-OFF$Loggers
  private def logResponse: Int => Unit = {
    case OK     => logger.info("Subscription display request is successful")
    case status => logger.warn(s"Subscription display request is failed with status $status")
  }
  // $COVERAGE-ON

  private def makeQueryString(queryParams: Seq[(String, String)]): String = {
    val params: String = queryParams map Function.tupled((k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}") mkString "&"
    if (params.isEmpty) "" else s"?$params"
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
