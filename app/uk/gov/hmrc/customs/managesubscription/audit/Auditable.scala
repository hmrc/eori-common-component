/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.customs.managesubscription.audit

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent, ExtendedDataEvent}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class Auditable @Inject() (auditConnector: AuditConnector, @Named("appName") appName: String)(implicit
  ec: ExecutionContext
) {

  private val auditSource: String = appName

  private val audit: Audit = Audit(auditSource, auditConnector)

  def sendDataEvent(transactionName: String, path: String, detail: Map[String, String], auditType: String)(implicit
    hc: HeaderCarrier
  ): Unit =
    audit.sendDataEvent(
      DataEvent(
        auditSource,
        auditType,
        tags = hc.toAuditTags(transactionName, path),
        detail = hc.toAuditDetails(detail.toSeq: _*)
      )
    )

  def sendExtendedDataEvent(
    transactionName: String,
    path: String,
    tags: Map[String, String] = Map.empty,
    details: JsValue,
    eventType: String
  )(implicit hc: HeaderCarrier): Unit =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(auditSource, eventType, tags = hc.toAuditTags(transactionName, path) ++ tags, detail = details)
    )

}
