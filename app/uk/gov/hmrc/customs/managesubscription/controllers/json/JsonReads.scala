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

package uk.gov.hmrc.customs.managesubscription.controllers.json

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, _}
import uk.gov.hmrc.customs.managesubscription.domain.SubscriptionCompleteStatus.SubscriptionCompleteStatus
import uk.gov.hmrc.customs.managesubscription.domain.{SubscriptionComplete, SubscriptionCompleteStatus}

import scala.util.Try

object JsonReads {

  private implicit val subscriptionCompleteStatusReads: Reads[SubscriptionCompleteStatus] =
    Reads[SubscriptionCompleteStatus] {
      case JsString(s) =>
        Try(SubscriptionCompleteStatus.withName(s)).toOption
          .fold[JsResult[SubscriptionCompleteStatus]](ifEmpty = JsError())(JsSuccess(_))
      case _ => JsError()
    }

  implicit val subscriptionCompleteReads: Reads[SubscriptionComplete] = (
    (__ \ "url").read[String] and
      (__ \ "state").read[SubscriptionCompleteStatus] and
      (__ \ "errorResponse").readNullable[String]
  )(SubscriptionComplete.apply _)

}
