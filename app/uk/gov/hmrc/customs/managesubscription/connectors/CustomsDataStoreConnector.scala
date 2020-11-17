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

package uk.gov.hmrc.customs.managesubscription.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDataStoreConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def storeEmailAddress(dataStoreRequest: DataStoreRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val query =
      s"""{"query" : "mutation {byEori(eoriHistory:{eori:\\"${dataStoreRequest.eori}\\"}, notificationEmail:{address:\\"${dataStoreRequest.email}\\", timestamp:\\"${dataStoreRequest.emailVerificationTimestamp}\\"})}"}"""
    val header = hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.customDataStoreToken}")))
    val url    = appConfig.customDataStoreUrl

    // $COVERAGE-OFF$Loggers
    logger.debug(s"[StoreEmailAddress: $url, body: $query and headers: $header")
    // $COVERAGE-ON

    auditRequest(dataStoreRequest, appConfig.customDataStoreUrl)

    httpClient.doPost[JsValue](
      appConfig.customDataStoreUrl,
      Json.parse(query),
      Seq("Content-Type" -> "application/json")
    )(implicitly, header, ec)
      .map { response =>
        auditResponse(response, appConfig.customDataStoreUrl)
        logResponse(response)
        response
      }
  }

  private def logResponse(response: HttpResponse): Unit = response.status match {
    case OK => logger.info("CustomsDataStore: data store request is successful")
    case _  => logger.warn(s"CustomsDataStore: data store request is failed with response $response")
  }

  private def auditRequest(request: DataStoreRequest, url: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "DataStoreRequestSubmitted",
      path = url,
      detail = Map("eori number" -> s"${request.eori}", "emailAddress" -> s"${request.email}"),
      auditType = "DataStoreRequest"
    )

  private def auditResponse(response: HttpResponse, url: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "DataStoreResponseReceived",
      path = url,
      detail = Map("status" -> s"${response.status}", "message" -> s"${response.body}"),
      auditType = "DataStoreResponse"
    )

}
