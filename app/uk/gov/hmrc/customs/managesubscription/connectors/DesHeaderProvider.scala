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

import com.google.inject.Singleton
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, DATE, X_FORWARDED_HOST}
import play.api.http.MimeTypes

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.UUID

@Singleton
class DesHeaderProvider {

  private val clock: Clock = Clock.systemDefaultZone()

  def generateHeadersWithBearerToken(bearerToken: String): Seq[(String, String)] =
    Seq(
      DATE               -> DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock.withZone(ZoneId.of("GMT")))),
      "X-Correlation-ID" -> UUID.randomUUID().toString,
      X_FORWARDED_HOST   -> "MDTP",
      ACCEPT             -> MimeTypes.JSON,
      AUTHORIZATION      -> s"Bearer $bearerToken"
    )

}
