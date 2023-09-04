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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status.OK
import uk.gov.hmrc.customs.managesubscription.BuildUrl
import uk.gov.hmrc.customs.managesubscription.domain.vat.VatCustomerInformation
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetVatCustomerInformationConnector @Inject() (buildUrl: BuildUrl, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceName: String = "integration-framework"

  private val baseUrl = buildUrl(serviceName)

  def getVatCustomerInformation(
    vrn: String
  )(implicit hc: HeaderCarrier): EitherT[Future, HttpResponse, VatCustomerInformation] = EitherT {

    val vatUrl: URL = url"$baseUrl/vat/customer/vrn/$vrn/information"

    logger.info(s"[$serviceName][Connector] GET url: $vatUrl")

    httpClient.get(vatUrl)
      .execute map {
      response =>
        logger.debug(s"getVatCustomerInformation successful. url: $vatUrl")
        response.status match {
          case OK => Right(response.json.as[VatCustomerInformation])
          case _  => Left(response)
        }
    }
  }

}
