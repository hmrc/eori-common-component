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

package uk.gov.hmrc.customs.managesubscription.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.models.events.{EmailCall, EmailResponse}
import uk.gov.hmrc.customs.managesubscription.services.dto.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EmailConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient, audit: Auditable) {

  private val logger = Logger(this.getClass)

  def sendEmail(email: Email)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url = appConfig.emailServiceUrl

    // $COVERAGE-OFF$Loggers
    logger.debug(s"SendEmail: $url, body: $email and headers: $hc")
    // $COVERAGE-ON

    httpClient.POST[Email, HttpResponse](url, email, Seq("Content-Type" -> "application/json")).map {
      response =>
        audit(email, response, appConfig.emailServiceUrl)
        logResponse(email.templateId, response)
        response
    }
  }

  private def logResponse(templateId: String, response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug(s"sendEmail succeeded for template Id: $templateId")
    else
      logger.warn(s"sendEmail: request is failed with $response for template Id: $templateId")

  private def audit(email: Email, response: HttpResponse, url: String)(implicit hc: HeaderCarrier): Future[Unit] =
    Future.successful {
      audit.sendExtendedDataEvent(
        transactionName = "ecc-email-call",
        path = url,
        details = Json.toJson(EmailCall(email, EmailResponse(response))),
        eventType = "EmailCall"
      )
    }

}
