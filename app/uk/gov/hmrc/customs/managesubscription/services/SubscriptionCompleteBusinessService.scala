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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.managesubscription.CdsLogger.logger
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.connectors.{CustomsDataStoreConnector, SubscriptionDisplayConnector}
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.SubscriptionCompleteStatus
import uk.gov.hmrc.customs.managesubscription.domain.{
  DataStoreRequest,
  RecipientDetailsWithEori,
  SubscriptionComplete,
  SubscriptionCompleteStatus
}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionCompleteBusinessService @Inject() (
  recipientDetailsStore: RecipientDetailsStore,
  emailService: EmailService,
  audit: Auditable,
  dataStoreConnector: CustomsDataStoreConnector,
  subscriptionDisplayConnector: SubscriptionDisplayConnector
) {

  def onSubscriptionStatus(subscriptionComplete: SubscriptionComplete, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    subscriptionComplete.state match {
      case status @ SubscriptionCompleteStatus.SUCCEEDED =>
        auditStatus(status, formBundleId)
        sendAndStoreEmail(status, formBundleId)
      case status @ SubscriptionCompleteStatus.ERROR =>
        auditStatus(status, formBundleId)
        sendEmail(status, formBundleId)
      case badState =>
        auditStatus(badState, formBundleId)
        triggerEnrolmentStateIssueAlert()
    }

  private def triggerEnrolmentStateIssueAlert(): Future[Unit] = {
    val MessageThatTriggersPagerDutyAlert = "TAX_ENROLMENT_STATE_ISSUE"
    Future.successful(logger.error(MessageThatTriggersPagerDutyAlert))
  }

  private def sendAndStoreEmail(status: SubscriptionCompleteStatus, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    for {
      recipient  <- recipientDetailsStore.recipientDetailsForBundleId(formBundleId)
      _          <- emailService.sendEmail(recipient.recipientDetails, status)
      eoriNumber <- retrieveEori(recipient)
      _          <- Future.sequence(eoriNumber.map(dataStoreEmailRequest(recipient)).toList)
    } yield (): Unit

  private def sendEmail(status: SubscriptionCompleteStatus, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    for {
      recipient <- recipientDetailsStore.recipientDetailsForBundleId(formBundleId)
      _         <- emailService.sendEmail(recipient.recipientDetails, status)
    } yield (): Unit

  private def dataStoreEmailRequest(
    recipient: RecipientDetailsWithEori
  )(implicit hc: HeaderCarrier): String => Future[HttpResponse] = eori =>
    dataStoreConnector.storeEmailAddress(
      DataStoreRequest(eori, recipient.recipientDetails.recipientEmailAddress, recipient.emailVerificationTimestamp)
    )

  private def retrieveEori(recipient: RecipientDetailsWithEori)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    lazy val buildQueryParams: List[(String, String)] = {
      val generateUUIDAsString: String = UUID.randomUUID().toString.replace("-", "")
      List("regime" -> "CDS", "acknowledgementReference" -> generateUUIDAsString)
    }

    if (recipient.eori.isDefined)
      Future.successful(recipient.eori)
    else
      subscriptionDisplayConnector.callSubscriptionDisplay(("taxPayerID" -> recipient.safeId) :: buildQueryParams)
  }

  private def auditStatus(status: SubscriptionCompleteStatus, formBundleId: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      transactionName = "eori-common-component-update-status",
      path = s"/eori-common-component/$formBundleId",
      detail = Map("state" -> status.toString, "formBundleId" -> formBundleId),
      auditType = "taxEnrolmentStatus"
    )

}
