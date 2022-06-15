/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.Status._
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.domain.DataStoreRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDataStoreConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def updateDataStore(dataStoreRequest: DataStoreRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    auditRequest(dataStoreRequest, appConfig.customDataStoreUrl)

    httpClient.POST[DataStoreRequest, HttpResponse](appConfig.customDataStoreUrl, dataStoreRequest)
      .map { response =>
        auditResponse(response, appConfig.customDataStoreUrl)
        logResponse(response.status)
        response
      }
  }

  private def logResponse: Int => Unit = {
    case NO_CONTENT => logger.info("CustomsDataStore: data store request is successful")
    case status     => logger.warn(s"CustomsDataStore: data store request is failed with status $status")
  }

  private def auditRequest(request: DataStoreRequest, url: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "DataStoreRequestSubmitted",
      path = url,
      detail = Map(
        "eori number"  -> s"${request.eori}",
        "emailAddress" -> s"${request.address}",
        "timestamp"    -> s"${request.timestamp}"
      ),
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
