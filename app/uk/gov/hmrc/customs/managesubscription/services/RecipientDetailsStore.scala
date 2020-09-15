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

package uk.gov.hmrc.customs.managesubscription.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.cache.model.Id
import uk.gov.hmrc.customs.managesubscription.domain.protocol.Eori
import uk.gov.hmrc.customs.managesubscription.domain.{RecipientDetails, RecipientDetailsWithEori}
import uk.gov.hmrc.customs.managesubscription.repository.RecipientDetailsRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RecipientDetailsStore @Inject() (repository: RecipientDetailsRepository) {

  def saveRecipientDetailsForBundleId(
    formBundleId: String,
    eori: Option[Eori],
    recipientDetails: RecipientDetails,
    emailVerificationTimestamp: String,
    safeId: String
  ): Future[Unit] =
    repository.saveRecipientDetailsForBundleId(formBundleId, eori, recipientDetails, emailVerificationTimestamp, safeId)

  def recipientDetailsForBundleId(formBundleId: String): Future[RecipientDetailsWithEori] = getCached(formBundleId)

  private def getCached(formBundleId: Id): Future[RecipientDetailsWithEori] =
    repository.recipientDetailsForBundleId(formBundleId.id).map {
      case Right(recipientDetailsWithEori) => recipientDetailsWithEori
      case Left(_) =>
        throw new IllegalStateException("Unable to process the recipientDetails, recipientDetailsWithEori expected")
    }

}
