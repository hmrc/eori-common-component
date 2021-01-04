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

package uk.gov.hmrc.customs.managesubscription.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.managesubscription.config.AppConfig
import uk.gov.hmrc.customs.managesubscription.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.customs.managesubscription.domain.protocol.{Eori, TaxEnrolmentsRequest}
import uk.gov.hmrc.customs.managesubscription.domain.{RecipientDetails, TaxPayerId}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsService @Inject() (
  appConfig: AppConfig,
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  recipientDetailsStore: RecipientDetailsStore
) {

  def saveRecipientDetailsAndCallTaxEnrolment(
    formBundleId: String,
    recipientDetails: RecipientDetails,
    sapNumber: TaxPayerId,
    eori: Option[Eori],
    emailVerificationTimestamp: String,
    safeId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    def saveRecipientDetails(): Future[Unit] =
      recipientDetailsStore.saveRecipientDetailsForBundleId(
        formBundleId,
        eori,
        recipientDetails,
        emailVerificationTimestamp,
        safeId
      )

    def taxEnrolmentsRequest =
      TaxEnrolmentsRequest(
        recipientDetails.enrolmentKey,
        s"${appConfig.taxEnrolmentsCallbackUrl}/$formBundleId",
        sapNumber.id
      )

    saveRecipientDetails().flatMap { _ =>
      taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId)
    }
  }

}
