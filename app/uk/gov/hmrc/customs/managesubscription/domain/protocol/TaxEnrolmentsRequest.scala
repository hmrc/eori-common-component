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

package uk.gov.hmrc.customs.managesubscription.domain.protocol

import play.api.libs.json.{Json, OFormat}

case class TaxEnrolmentsRequest(serviceName: String, callback: String, etmpId: String, confidenceLevel: Option[String] = None)

object TaxEnrolmentsRequest {
  implicit val jsonFormat: OFormat[TaxEnrolmentsRequest] = Json.format[TaxEnrolmentsRequest]
}

case class Email(value: String)

object Email {
  implicit val jsonFormat = Json.format[Email]
}

case class Eori(value: String)

object Eori {
  implicit val jsonFormat = Json.format[Eori]
}