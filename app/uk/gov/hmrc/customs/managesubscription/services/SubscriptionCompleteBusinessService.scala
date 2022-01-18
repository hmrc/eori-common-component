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

package uk.gov.hmrc.customs.managesubscription.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.customs.managesubscription.audit.Auditable
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.SubscriptionCompleteStatus
import uk.gov.hmrc.customs.managesubscription.domain.{SubscriptionComplete, SubscriptionCompleteStatus}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionCompleteBusinessService @Inject() (
  recipientDetailsStore: RecipientDetailsStore,
  emailService: EmailService,
  audit: Auditable
) {

  private val logger = Logger(this.getClass)

  def onSubscriptionStatus(subscriptionComplete: SubscriptionComplete, formBundleId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    subscriptionComplete.state match {
      case status @ SubscriptionCompleteStatus.SUCCEEDED =>
        auditStatus(status, formBundleId, subscriptionComplete.errorResponse)
        sendAndStoreSuccessEmail(formBundleId)
      case status @ SubscriptionCompleteStatus.ERROR =>
        auditStatus(status, formBundleId, subscriptionComplete.errorResponse)
        sendFailureEmail(formBundleId)
      case badState =>
        auditStatus(badState, formBundleId, subscriptionComplete.errorResponse)
        triggerEnrolmentStateIssueAlert(subscriptionComplete)
    }

  private def triggerEnrolmentStateIssueAlert(subscriptionComplete: SubscriptionComplete): Future[Unit] = {
    val messageThatTriggersPagerDutyAlert = s"TAX_ENROLMENT_STATE_ISSUE - $subscriptionComplete"
    Future.successful(logger.error(messageThatTriggersPagerDutyAlert))
  }

  private def sendAndStoreSuccessEmail(formBundleId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      recipient <- recipientDetailsStore.recipientDetailsForBundleId(formBundleId)
      _         <- emailService.sendSuccessEmail(recipient.recipientDetails)
    } yield (): Unit

  private def sendFailureEmail(formBundleId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      recipient <- recipientDetailsStore.recipientDetailsForBundleId(formBundleId)
      _         <- emailService.sendFailureEmail(recipient.recipientDetails)
    } yield (): Unit

  private def auditStatus(status: SubscriptionCompleteStatus, formBundleId: String, errorResponse: Option[String])(
    implicit hc: HeaderCarrier
  ): Unit = {
    val details = Map("state" -> status.toString, "formBundleId" -> formBundleId)

    audit.sendDataEvent(
      transactionName = "eori-common-component-update-status",
      path = s"/eori-common-component/$formBundleId",
      detail = if (errorResponse.isDefined) details + ("errorResponse" -> errorResponse.get) else details,
      auditType = "taxEnrolmentStatus"
    )
  }

}
