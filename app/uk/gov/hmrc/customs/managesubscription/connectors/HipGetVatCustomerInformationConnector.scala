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

import cats.data.EitherT
import cats.implicits.catsSyntaxEq
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
import uk.gov.hmrc.customs.managesubscription.BuildUrl
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.domain.vat.{
  HipCustomErrResponse,
  HipVatCustomerInformationSuccessWrapper,
  VatCustomerInformation
}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipGetVatCustomerInformationConnector @Inject() (
  buildUrl: BuildUrl,
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  headerProvider: DesHeaderProvider
)(implicit ec: ExecutionContext)
    extends Logging with HandleResponses with GetVatCustomerInformationConnector {

  val serviceName: String = "hip"

  private val baseUrl = buildUrl(serviceName)

  def getVatCustomerInformation(vrn: String): EitherT[Future, ResponseError, VatCustomerInformation] = EitherT {

    val vatUrl: URL = url"$baseUrl/etmp/RestAdapter/vat/subscription/vrn/$vrn"

    logger.info(s"[$serviceName][Connector] GET url: $vatUrl")

    def headers: Seq[(String, String)] =
      Seq(
        "correlationid"         -> UUID.randomUUID().toString,
        "X-Originating-System"  -> "MDTP",
        "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
        "X-Transmitting-System" -> "HIP",
        "Authorization"         -> s"Basic ${appConfig.hipAuthorizationToken}"
      )

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

    val correlationId = hc.extraHeaders.find((key, _) => key == "correlationid")

    httpClient.get(vatUrl)
      .execute map {
      response =>
        logger.info(s"hipGetVatCustomerInformation successful. response: ${response.status}")
        val error = s"Unexpected status from getVatCustomerInformation: ${response.status} body: ${response.body}"
        response.status match {
          case OK => handleResponse[HipVatCustomerInformationSuccessWrapper](response).map(_.success)
          case UNPROCESSABLE_ENTITY =>
            val code = response.json.as[HipCustomErrResponse].error.code
            logger.warn(s"Unprocessable Entity(${response.status}) returned from HIP with body: ${response.body}")
            if (code === "002")
              logger.warn(s"Transforming a status: ${response.status} with code: $code to a $NOT_FOUND")
              // Transforming error as FE expects a 404 for a specific error page
              Left(ResponseError(NOT_FOUND, error))
            else
              Left(ResponseError(response.status, error))
          case _ =>
            logger.warn(s"$error correlationId: $correlationId")
            Left(ResponseError(response.status, error))
        }
    }
  }

}
